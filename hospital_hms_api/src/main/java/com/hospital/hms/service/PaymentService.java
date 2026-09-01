package com.hospital.hms.service;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface PaymentService {
    ObjectNode unifiedOrder(String outTradeNo, String openId, int total, String desc, String notifyUrl, String timeExpire);

}
