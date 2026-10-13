package com.bloodconnect.admin;

import com.bloodconnect.donor.DonorProfile;
import com.bloodconnect.donor.DonorProfileRepository;
import com.bloodconnect.request.BloodRequest;
import com.bloodconnect.request.BloodRequestRepository;
import com.bloodconnect.request.RequestStatus;
import com.bloodconnect.user.AppUser;
import com.bloodconnect.user.UserRepository;
import com.bloodconnect.user.UserRole;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final BloodRequestRepository requestRepository;

    public AdminService(UserRepository userRepository, DonorProfileRepository donorProfileRepository,
                        BloodRequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.requestRepository = requestRepository;
    }

    public AdminOverviewResponse overview() {
        return new AdminOverviewResponse(
                userRepository.countByRole(UserRole.HOSPITAL),
                userRepository.countByRole(UserRole.DONOR),
                userRepository.countByRole(UserRole.REQUESTER),
                requestRepository.countByStatus(RequestStatus.FULFILLED));
    }

    public List<AdminHospitalResponse> hospitals() {
        Map<UUID, List<BloodRequest>> requestsByUser = requestRepository.findAll().stream()
                .collect(Collectors.groupingBy(request -> request.getRequester().getId()));
        return userRepository.findAllByRoleOrderByCreatedAtDesc(UserRole.HOSPITAL).stream()
                .map(user -> {
                    List<BloodRequest> requests = requestsByUser.getOrDefault(user.getId(), List.of());
                    long helped = requests.stream().filter(r -> r.getStatus() == RequestStatus.FULFILLED).count();
                    return new AdminHospitalResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                            requests.size(), helped, user.getCreatedAt());
                })
                .toList();
    }

    public List<AdminDonorResponse> donors() {
        Map<UUID, DonorProfile> profiles = donorProfileRepository.findAll().stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));
        return userRepository.findAllByRoleOrderByCreatedAtDesc(UserRole.DONOR).stream()
                .map(user -> donor(user, profiles.get(user.getId())))
                .toList();
    }

    public List<AdminHelpedResponse> helped() {
        return requestRepository.findAllByStatusOrderByCreatedAtDesc(RequestStatus.FULFILLED).stream()
                .map(request -> {
                    AppUser requester = request.getRequester();
                    return new AdminHelpedResponse(request.getId(), request.getPatientName(),
                            request.getBloodGroup(), request.getUnits(), request.getHospital(),
                            request.getLocation(), request.getUrgency(), requester.getName(),
                            requester.getEmail(), requester.getRole(), request.getCreatedAt());
                })
                .toList();
    }

    private static AdminDonorResponse donor(AppUser user, DonorProfile profile) {
        if (profile == null) {
            return new AdminDonorResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                    null, null, null, user.getCreatedAt());
        }
        return new AdminDonorResponse(user.getId(), profile.getName(), user.getEmail(), profile.getPhone(),
                profile.getBloodGroup(), profile.getLocation(), profile.isAvailable(), user.getCreatedAt());
    }
}
