package com.hospital.hms.service.impl;

import cn.felord.payment.PayException;
import cn.felord.payment.wechat.v3.WechatResponseEntity;
import cn.felord.payment.wechat.v3.model.Amount;
import cn.felord.payment.wechat.v3.model.PayParams;
import cn.felord.payment.wechat.v3.model.Payer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hospital.hms.service.PaymentService;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {
//
//    @Resource
//    private WechatApiProvider wechatApiProvider;

    /**
     * outTradeNo：挂号单的流水号
     * openId：患者的openid
     * total：挂号单金额
     * desc：挂号单描述信息
     * notifyUrl：用于接收付款结果的URL地址
     * timeExpire：挂号单的过期时间
     * */
    @Override
    public ObjectNode unifiedOrder(String outTradeNo, String openId, int total, String desc, String notifyUrl, String timeExpire) {
        PayParams params = new PayParams();
        //设置付款人
        Payer payer = new Payer();
        payer.setOpenid(openId);
        params.setPayer(payer);

        //设置金额（货币单位是分）
        Amount amount = new Amount();
        //amount.setTotal(total);
        amount.setTotal(1);
        params.setAmount(amount);
        params.setOutTradeNo(outTradeNo);
        params.setDescription(desc);
        params.setNotifyUrl(notifyUrl);
        if (timeExpire != null) {
            params.setTimeExpire(timeExpire);
        }
        //创建支付订单
//        WechatResponseEntity<ObjectNode> response = wechatApiProvider.directPayApi("patient-wx-api").jsPay(params);
        WechatResponseEntity<ObjectNode> response = null;
        if (response.is2xxSuccessful()) {
            ObjectNode json = response.getBody();
            return json;
        } else {
            throw new PayException("创建微信支付订单失败");
        }
    }
}
