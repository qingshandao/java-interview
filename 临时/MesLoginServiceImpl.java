package com.tst.plc.mes.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tst.plc.common.constants.PlcDataType;
import com.tst.plc.common.dto.Res;
import com.tst.plc.common.plc.service.impl.PlcContext;
import com.tst.plc.mes.dto.MesLoginDTO;
import com.tst.plc.mes.entity.MesDataLogs;
import com.tst.plc.mes.entity.MesInterfaceConfig;
import com.tst.plc.mes.entity.MesPlan;
import com.tst.plc.mes.mapper.MesDataLogsMapper;
import com.tst.plc.mes.mapper.MesInterfaceMapper;
import com.tst.plc.mes.service.IMesLoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 芜湖MES：请求MES登录接口，提交用户工号和密码，判断当前用户是否登录成功
 * </p>
 *
 * @author gongcheng
 * @since 2025-08-18
 */
@Service
@Slf4j
public class MesLoginServiceImpl implements IMesLoginService {

    @Autowired
    private HttpRequestService httpRequestService;

    @Resource
    private MesInterfaceMapper mesInterfaceMapper;

    @Resource
    private PlcContext plcContext;

    @Resource
    private MesDataLogsMapper mesDataLogsMapper;

    /**
     * 请求
     * @param mesLoginDTO
     * @return
     */
    @Override
    public String loginMes(MesLoginDTO mesLoginDTO) {
        // 1、准备数据
        QueryWrapper<MesInterfaceConfig> mesIpWrapper = new QueryWrapper<>();
        mesIpWrapper.eq("description","CXDoLogin");
        MesInterfaceConfig mesInterfaceConfig = this.mesInterfaceMapper.selectOne(mesIpWrapper);

        String mesLoginIp = mesInterfaceConfig.getIp();
        String mesLoginPath = mesInterfaceConfig.getPath();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("USER", mesLoginDTO.getUSER());
        params.add("Password", mesLoginDTO.getPassword());

        // 2、请求MES登录接口
        String result = httpRequestService.sendPost(mesLoginIp, mesLoginPath, params);

        // 3.解析获取到的数据
        String nickName = readNickName(result);

        // 4.记录日志
        // 4.1. 请求参数
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("USER", params.getFirst("USER"));
        requestMap.put("Password",params.getFirst("Password"));
        String requestJson = JSON.toJSONString(requestMap);
        // 4.2. 响应参数
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("nickName", nickName);
        String responseJson = JSON.toJSONString(responseMap);
        // 4.3. 日志数据
        MesDataLogs mesDataLogs = new MesDataLogs();
        mesDataLogs.setInterfaceName("CXDoLogin");
        mesDataLogs.setRequestParams(requestJson);
        mesDataLogs.setResponseData(responseJson);
        mesDataLogs.setSuccess("".equals(nickName) ? 0 : 1);
        mesDataLogs.setCreateTime(new Date());
        mesDataLogsMapper.insert(mesDataLogs);

        // 5.将MES用户的工号下载到PLC
        plcContext.getPlcOperationApiService().writePlcByType("Mes_User_Num", mesLoginDTO.getUSER(), PlcDataType.STRING_TYPE);

        // 直接返回请求MES登录的结果
        return nickName;
    }

    public String readNickName(String response) {
        String nickName = "0";

        try {
            // 解析 XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // 处理命名空间
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(response.getBytes("UTF-8")));

            // 获取根节点（就是 <string>）
            Node root = doc.getDocumentElement();
            nickName = root.getTextContent();
        }catch (Exception e){
            log.error("===== MES登录响应数据解析失败 ====");
            log.error(e.getLocalizedMessage());
        }

        return nickName;
    }
}
