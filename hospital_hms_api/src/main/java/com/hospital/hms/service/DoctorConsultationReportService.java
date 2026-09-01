package com.hospital.hms.service;

import java.util.HashMap;
import java.util.Map;

public interface DoctorConsultationReportService {
    HashMap selectConsultationReportByRegistrationId(Map param);
}
