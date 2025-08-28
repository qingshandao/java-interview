package com.tst.plc.mes.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tst.plc.common.constants.PlcDataType;
import com.tst.plc.common.dto.Res;
import com.tst.plc.common.plc.service.impl.PlcContext;
import com.tst.plc.mes.dto.MesSetBarCodeDTO;
import com.tst.plc.mes.entity.MesDataLogs;
import com.tst.plc.mes.entity.MesInterfaceConfig;
import com.tst.plc.mes.entity.MesPlan;
import com.tst.plc.mes.mapper.MesDataLogsMapper;
import com.tst.plc.mes.mapper.MesInterfaceMapper;
import com.tst.plc.mes.mapper.MesPlanListMapper;
import com.tst.plc.mes.service.IMesSetBarCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class MesSetBarCodeServiceImpl implements IMesSetBarCode {

    @Autowired
    private HttpRequestService httpRequestService;

    @Resource
    private MesInterfaceMapper mesInterfaceMapper;

    @Resource
    private MesPlanListMapper mesPlanListMapper;

    @Resource
    private PlcContext plcContext;

    @Resource
    private MesDataLogsMapper mesDataLogsMapper;

    private final Map<String, String> MES_BARCODE_RESULT = new HashMap<>();
    {
        MES_BARCODE_RESULT.put("0", "绑定失败");
        MES_BARCODE_RESULT.put("1", "绑定成功");
        MES_BARCODE_RESULT.put("2", "未找到条码");
        MES_BARCODE_RESULT.put("3", "条码已绑定");
        MES_BARCODE_RESULT.put("4", "未找到机台");
        MES_BARCODE_RESULT.put("5", "未找到班次");
    }

    @Override
    public Integer setBarCode() {
        MesDataLogs mesDataLogs = new MesDataLogs();
        mesDataLogs.setInterfaceName("setWF_02");
        mesDataLogs.setCreateTime(new Date());
        Integer requestSuccess = 1;

        // 1.获取数据
        MesSetBarCodeDTO data = null;
        try {
            data = getData();
        }catch (Exception e){
            log.error("=== MesSetBarCodeServiceImpl.setBarCode()--- 获取当前Plan 或 PLC数据失败！！！ ===");
            e.printStackTrace();
            requestSuccess = 0;
            mesDataLogs.setRequestParams(JSON.toJSONString(data == null ? "" : data));
            mesDataLogs.setSuccess(requestSuccess);
            mesDataLogsMapper.insert(mesDataLogs);
        }

        // 获取请求MES所需要的数据失败
        if(data != null && !data.getGetAllDataSuccess()){
            requestSuccess = 0;
            mesDataLogs.setRequestParams(JSON.toJSONString(data));
            mesDataLogs.setSuccess(requestSuccess);
            mesDataLogsMapper.insert(mesDataLogs);
            return null;
        }

        // 3.发送MES条码绑定请求
        String response = sendRequestToMes(data);

        if(!"".equals(response)){
            // 4.解析返回数据
            String result = "0";
            try {
                result = readResponse(response);
            } catch (Exception e) {
                log.error("MesSetBarCodeServiceImpl.setBarCode(): 解析MES请求结果失败！", e.getMessage());
                e.printStackTrace();
                requestSuccess = 0;
            }finally {
                mesDataLogs.setRequestParams(JSON.toJSONString(data));
                mesDataLogs.setResponseData(JSON.toJSONString(result));
                mesDataLogs.setSuccess(requestSuccess);
                mesDataLogsMapper.insert(mesDataLogs);
            }

            // 5.响应结果下发给PLC
            plcContext.getPlcOperationApiService().writePlcByType("MES_Bind_Barcode_Result", result, PlcDataType.INT_TYPE);

            return Integer.valueOf(result);
        }

       return 0;
    }

    /**
     * 重新绑定条码
     * @param newBarCode
     * @return
     */
    @Override
    public Res<?> reBindBarCode(String newBarCode) {
        MesDataLogs mesDataLogs = new MesDataLogs();
        mesDataLogs.setInterfaceName("setWF_02");
        mesDataLogs.setCreateTime(new Date());
        Integer requestSuccess = 1;

        // 将新条码下发到PLC
        plcContext.getPlcOperationApiService().writePlcByType("MES_Bind_BarCode", newBarCode, PlcDataType.STRING_TYPE);

        MesSetBarCodeDTO data = getData();
        if(data.getGetAllDataSuccess()){
            data.setBarCode(newBarCode);
        }else{
            requestSuccess = 0;
            mesDataLogs.setRequestParams(JSON.toJSONString(data));
            mesDataLogs.setSuccess(requestSuccess);
            mesDataLogsMapper.insert(mesDataLogs);
            return Res.fail("获取 当前计划 或 PLC数据 失败！！");
        }

        String response = sendRequestToMes(data);
        if(!"".equals(response)){
            String result = "0";
            try {
                result = readResponse(response);
            } catch (Exception e) {
                log.error("MesSetBarCodeServiceImpl.setBarCode(): 解析MES请求结果失败！", e.getMessage());
                e.printStackTrace();
                return Res.fail("解析MES响应数据失败！");
            }finally {
                mesDataLogs.setRequestParams(JSON.toJSONString(data));
                mesDataLogs.setResponseData(JSON.toJSONString(result));
                mesDataLogs.setSuccess(requestSuccess);
                mesDataLogsMapper.insert(mesDataLogs);
            }

            // 5.响应结果下发给PLC
            plcContext.getPlcOperationApiService().writePlcByType("MES_Bind_Barcode_Result", result, PlcDataType.INT_TYPE);
            HashMap<String, String> resultMap = new HashMap<>();
            resultMap.put("code",result);
            resultMap.put("msg","MES绑定结果：" + MES_BARCODE_RESULT.get(result));
            return Res.success(resultMap);
        }

        return Res.fail("请求MES绑定条码接口失败！！");
    }


    public Object getTagFromPlc(String tagName, String plcDataType){
        Object tagValue = plcContext.getPlcOperationApiService().readPlcByType(tagName, plcDataType, (short) 1);
        if(tagValue == null){
            log.error("MesSetBarCodeServiceImpl.getTagFromPlc(): 没有从PLC获取到"+ tagName + "标签的值");
        }

        return tagValue;
    }

    public MesSetBarCodeDTO getData(){
        MesSetBarCodeDTO mesSetBarCodeDTO = new MesSetBarCodeDTO();
        mesSetBarCodeDTO.setGetAllDataSuccess(false);

        // 1.从PLC获取信息
        // 1.1。待绑定条码
        Object mes_bind_barCode = getTagFromPlc("MES_Bind_BarCode", PlcDataType.STRING_TYPE);
        if(mes_bind_barCode == null){
            log.error("MesSetBarCodeServiceImpl.getData(): 获取PLC条码失败");
            return mesSetBarCodeDTO;
        }
        String mesBindBarCode = String.valueOf(mes_bind_barCode);
        mesSetBarCodeDTO.setBarCode(mesBindBarCode);

        // 1.2.机台号
        Object machine_pos = getTagFromPlc("Machine_Pos", PlcDataType.STRING_TYPE);
        if(machine_pos == null){
            log.error("MesSetBarCodeServiceImpl.getData(): 获取PLC机台号失败");
            return mesSetBarCodeDTO;
        }
        String machinePos = String.valueOf(machine_pos);
        mesSetBarCodeDTO.setMachCode(machinePos);

        // 1.3.班次
        Object banCi_name = getTagFromPlc("BanCi_Name", PlcDataType.INT_TYPE);
        if(banCi_name == null){
            log.error("MesSetBarCodeServiceImpl.getData(): 获取PLC班次失败");
            return mesSetBarCodeDTO;
        }
        Integer banCiNum = Integer.valueOf(String.valueOf(banCi_name));
        String banCiName;
        switch (banCiNum) {
            case 1: banCiName = "早班"; break;
            case 2: banCiName = "中班"; break;
            case 3: banCiName = "夜班"; break;
            default: banCiName = "未知班次"; break;
        }
        mesSetBarCodeDTO.setBanCiName(banCiName);

        // 1.4. 员工工号
        Object mes_user_num = getTagFromPlc("Mes_User_Num", PlcDataType.STRING_TYPE);
        if(mes_user_num == null){
            log.error("MesSetBarCodeServiceImpl.getData(): 获取PLC员工工号失败");
            return mesSetBarCodeDTO;
        }
        String mesUserNum = String.valueOf(mes_user_num);
        mesSetBarCodeDTO.setAcctCode(mesUserNum);

        // 2.从当前使用的计划中，获取LIMT、PLAN_ID
        QueryWrapper<MesPlan> mesPlanQueryWrapper = new QueryWrapper<>();
        mesPlanQueryWrapper.eq("is_current_plan", 1);
        MesPlan mesPlanNow = mesPlanListMapper.selectOne(mesPlanQueryWrapper);
        if(mesPlanNow != null){
            String litm = mesPlanNow.getLitm();
            String planId = mesPlanNow.getPlanId();

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 格式化日期为字符串
            String formatTime = dateFormat.format(calendar.getTime());


            mesSetBarCodeDTO.setLitm(litm);
            mesSetBarCodeDTO.setPlanId(planId);
            mesSetBarCodeDTO.setMfgDate(formatTime);
            mesSetBarCodeDTO.setGetAllDataSuccess(true);

            return mesSetBarCodeDTO;
        }else {
            log.error("MesSetBarCodeServiceImpl.getData(): 查询MES计划数据失败！！");
            return mesSetBarCodeDTO;
        }

    }

    public String sendRequestToMes(MesSetBarCodeDTO data){
        QueryWrapper<MesInterfaceConfig> mesIpWrapper = new QueryWrapper<>();
        mesIpWrapper.eq("description","setWF_02");
        MesInterfaceConfig mesInterfaceConfig = this.mesInterfaceMapper.selectOne(mesIpWrapper);
        String mesSetBcIp = mesInterfaceConfig.getIp();
        String mesSetBcPath = mesInterfaceConfig.getPath();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("MACH_CODE", data.getMachCode());
        params.add("BC_CODE", data.getBarCode());
        params.add("LITM_CODE", data.getLitm());
        params.add("ACCT_CODE", data.getAcctCode());
        params.add("BANCI_NAME", data.getBanCiName());
        params.add("PLAN_ID", data.getPlanId());
        params.add("MFG_DATE", data.getMfgDate());

        String response = "";
        try {
            response = httpRequestService.sendPost(mesSetBcIp, mesSetBcPath, params);
        }catch (Exception e){
            log.error("MesSetBarCodeServiceImpl.setBarCode()：",e);
            e.printStackTrace();
        }

        return response;
    }

    /**
     * 解析MES绑定条码后的结果
     * @param response
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public String readResponse(String response) throws Exception {
        String result = "0";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // 支持命名空间
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(response.getBytes("UTF-8")));

        NodeList list = doc.getElementsByTagNameNS("DCHL", "int");
        result = list.item(0).getTextContent();

        return result;
    }
}
