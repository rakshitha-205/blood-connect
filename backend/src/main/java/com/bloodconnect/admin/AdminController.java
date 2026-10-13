package com.bloodconnect.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public AdminOverviewResponse overview() {
        return adminService.overview();
    }

    @GetMapping("/hospitals")
    public List<AdminHospitalResponse> hospitals() {
        return adminService.hospitals();
    }

    @GetMapping("/donors")
    public List<AdminDonorResponse> donors() {
        return adminService.donors();
    }

    @GetMapping("/helped")
    public List<AdminHelpedResponse> helped() {
        return adminService.helped();
    }
}
