package com.hospital.wx.service;

import java.util.HashMap;
import java.util.Map;

public interface DoctorConsultationReportService {
    HashMap selectConsultationReportByRegistrationId(Map param);
}
