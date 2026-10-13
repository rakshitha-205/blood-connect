-- BloodConnect PostgreSQL schema. Create the database first: CREATE DATABASE blood_connect;
-- Generated from the JPA entities; keep it in sync when entities change.

CREATE TABLE public.app_users (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    email character varying(254) NOT NULL,
    email_verified_at timestamp(6) with time zone,
    full_name character varying(100),
    password_hash character varying(100) NOT NULL,
    phone character varying(20),
    phone_verified_at timestamp(6) with time zone,
    role character varying(20) NOT NULL,
    CONSTRAINT app_users_role_check CHECK (((role)::text = ANY ((ARRAY['DONOR'::character varying, 'REQUESTER'::character varying, 'HOSPITAL'::character varying, 'ADMIN'::character varying, 'BLOOD_BANK'::character varying])::text[])))
);

CREATE TABLE public.blood_bank_stock (
    bank_id uuid NOT NULL,
    units integer NOT NULL,
    blood_group character varying(3) NOT NULL
);

CREATE TABLE public.blood_banks (
    id uuid NOT NULL,
    latitude double precision,
    location character varying(160) NOT NULL,
    longitude double precision,
    name character varying(120) NOT NULL,
    open_hours character varying(80),
    phone character varying(30) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    user_id uuid NOT NULL
);

CREATE TABLE public.blood_requests (
    id uuid NOT NULL,
    blood_group character varying(3) NOT NULL,
    contact character varying(30) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    hospital character varying(120) NOT NULL,
    latitude double precision,
    location character varying(160) NOT NULL,
    longitude double precision,
    message character varying(1000),
    patient_name character varying(100) NOT NULL,
    status character varying(16) NOT NULL,
    units integer NOT NULL,
    urgency character varying(16) NOT NULL,
    requester_id uuid NOT NULL,
    CONSTRAINT blood_requests_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'FULFILLED'::character varying, 'CANCELLED'::character varying])::text[])))
);

CREATE TABLE public.donor_profiles (
    id uuid NOT NULL,
    available boolean NOT NULL,
    blood_group character varying(3) NOT NULL,
    deferred_until date,
    eligibility_checked_at timestamp(6) with time zone,
    latitude double precision,
    location character varying(160) NOT NULL,
    longitude double precision,
    name character varying(100) NOT NULL,
    phone character varying(30) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    user_id uuid NOT NULL
);

CREATE TABLE public.otp_codes (
    id uuid NOT NULL,
    attempts integer NOT NULL,
    channel character varying(10) NOT NULL,
    code_hash character varying(100) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT otp_codes_channel_check CHECK (((channel)::text = ANY ((ARRAY['EMAIL'::character varying, 'PHONE'::character varying])::text[])))
);

CREATE TABLE public.password_reset_codes (
    id uuid NOT NULL,
    attempts integer NOT NULL,
    code_hash character varying(100) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    user_id uuid NOT NULL
);

CREATE TABLE public.request_acceptances (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    donor_id uuid NOT NULL,
    request_id uuid NOT NULL
);

CREATE TABLE public.request_declines (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    reason character varying(300) NOT NULL,
    donor_id uuid NOT NULL,
    request_id uuid NOT NULL
);

CREATE TABLE public.reviews (
    id uuid NOT NULL,
    comment character varying(500),
    created_at timestamp(6) with time zone NOT NULL,
    rating integer NOT NULL,
    request_id uuid NOT NULL,
    reviewee_id uuid NOT NULL,
    reviewer_id uuid NOT NULL
);

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT app_users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.blood_bank_stock
    ADD CONSTRAINT blood_bank_stock_pkey PRIMARY KEY (bank_id, blood_group);

ALTER TABLE ONLY public.blood_banks
    ADD CONSTRAINT blood_banks_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.blood_requests
    ADD CONSTRAINT blood_requests_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.donor_profiles
    ADD CONSTRAINT donor_profiles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.otp_codes
    ADD CONSTRAINT otp_codes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.password_reset_codes
    ADD CONSTRAINT password_reset_codes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.request_acceptances
    ADD CONSTRAINT request_acceptances_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.request_declines
    ADD CONSTRAINT request_declines_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT uk4vj92ux8a2eehds1mdvmks473 UNIQUE (email);

ALTER TABLE ONLY public.blood_banks
    ADD CONSTRAINT uk9l52hxi0u8nu7dt993clvilrg UNIQUE (user_id);

ALTER TABLE ONLY public.request_acceptances
    ADD CONSTRAINT uk_request_acceptances_request_donor UNIQUE (request_id, donor_id);

ALTER TABLE ONLY public.request_declines
    ADD CONSTRAINT uk_request_declines_request_donor UNIQUE (request_id, donor_id);

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT uk_reviews_request_reviewer_reviewee UNIQUE (request_id, reviewer_id, reviewee_id);

ALTER TABLE ONLY public.donor_profiles
    ADD CONSTRAINT ukg4i8jo94ircw8dchtf2e5rlmg UNIQUE (user_id);

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT fk2bjaxrlns15dtt5b4jxwgby38 FOREIGN KEY (reviewer_id) REFERENCES public.app_users(id);

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT fk2tbuexqyr56wxnepfagoypdqi FOREIGN KEY (request_id) REFERENCES public.blood_requests(id);

ALTER TABLE ONLY public.donor_profiles
    ADD CONSTRAINT fk4o26u1fado3bo1jcedey8g58l FOREIGN KEY (user_id) REFERENCES public.app_users(id);

ALTER TABLE ONLY public.request_declines
    ADD CONSTRAINT fk6wwx0mh6mho13fn75sjjahswf FOREIGN KEY (donor_id) REFERENCES public.donor_profiles(id);

ALTER TABLE ONLY public.otp_codes
    ADD CONSTRAINT fkafj437kvsj9hqbrnbj5ypkwd5 FOREIGN KEY (user_id) REFERENCES public.app_users(id);

ALTER TABLE ONLY public.request_declines
    ADD CONSTRAINT fkc9cvrd0fy6fqc6bh5hbmddswh FOREIGN KEY (request_id) REFERENCES public.blood_requests(id);

ALTER TABLE ONLY public.blood_requests
    ADD CONSTRAINT fkfyr7g79gndc2ntfwp9a3i7vpy FOREIGN KEY (requester_id) REFERENCES public.app_users(id);

ALTER TABLE ONLY public.request_acceptances
    ADD CONSTRAINT fkhapyp9go5jicjnu2qo0xs0wwj FOREIGN KEY (donor_id) REFERENCES public.donor_profiles(id);

ALTER TABLE ONLY public.blood_banks
    ADD CONSTRAINT fkhmmryqmk39ta8cjir2x16s5o4 FOREIGN KEY (user_id) REFERENCES public.app_users(id);

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT fkimj71wyjct28ebejs77phd7wg FOREIGN KEY (reviewee_id) REFERENCES public.app_users(id);

ALTER TABLE ONLY public.blood_bank_stock
    ADD CONSTRAINT fkjcs4ujavncwjcyqksd06p2cdx FOREIGN KEY (bank_id) REFERENCES public.blood_banks(id);

ALTER TABLE ONLY public.request_acceptances
    ADD CONSTRAINT fkjhq9sy176d5o1dga1skrjxeu6 FOREIGN KEY (request_id) REFERENCES public.blood_requests(id);

ALTER TABLE ONLY public.password_reset_codes
    ADD CONSTRAINT fkmmeq5rn27tgcc6tai1f5m44yg FOREIGN KEY (user_id) REFERENCES public.app_users(id);
