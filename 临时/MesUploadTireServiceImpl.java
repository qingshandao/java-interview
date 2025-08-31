package com.tst.plc.mes.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tst.plc.common.constants.PlcDataType;
import com.tst.plc.common.dto.Res;
import com.tst.plc.common.plc.service.impl.PlcContext;
import com.tst.plc.mes.dto.MesUploadTireDTO;
import com.tst.plc.mes.entity.MesDataLogs;
import com.tst.plc.mes.entity.MesInterfaceConfig;
import com.tst.plc.mes.entity.MesPlan;
import com.tst.plc.mes.mapper.MesDataLogsMapper;
import com.tst.plc.mes.mapper.MesInterfaceMapper;
import com.tst.plc.mes.mapper.MesPlanListMapper;
import com.tst.plc.mes.service.IMesUploadTireService;
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
import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class MesUploadTireServiceImpl implements IMesUploadTireService {
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

    private final Map<String, String> MES_TIRE_WEIGHT_RESULT = new HashMap<>();
    {
        MES_TIRE_WEIGHT_RESULT.put("0", "网络异常");
        MES_TIRE_WEIGHT_RESULT.put("1", "合格");
        MES_TIRE_WEIGHT_RESULT.put("2", "偏轻");
        MES_TIRE_WEIGHT_RESULT.put("3", "偏重");
        MES_TIRE_WEIGHT_RESULT.put("4", "条码异常");
        MES_TIRE_WEIGHT_RESULT.put("5", "物料号异常");
        MES_TIRE_WEIGHT_RESULT.put("6", "条码不存在");
        MES_TIRE_WEIGHT_RESULT.put("7", "标准重量查询失败");
        MES_TIRE_WEIGHT_RESULT.put("8", "SetBCWeight_Mes 接口调用失败");
    }


    @Override
    public Integer uploadTireWeight() {
        MesDataLogs mesDataLogs = new MesDataLogs();
        mesDataLogs.setInterfaceName("UpLoadTyreWeight");
        mesDataLogs.setCreateTime(new Date());
        Integer requestSuccess = 1;

        // 1.获取PLC数据
        MesUploadTireDTO dataFromPLC = null;
        try {
            dataFromPLC = getDataFromPLC();
        }catch (Exception e){
            log.error("MesUploadTireServiceImpl.uploadTireWeight(): 获取PLC数据失败！！！");
            e.printStackTrace();
            requestSuccess = 0;
            mesDataLogs.setRequestParams(JSON.toJSONString(dataFromPLC == null ? "" : dataFromPLC));
            mesDataLogs.setSuccess(requestSuccess);
            mesDataLogsMapper.insert(mesDataLogs);
        }

        // 获取请求MES所需要的数据失败
        if(dataFromPLC != null && !dataFromPLC.getGetAllDataSuccess()){
            requestSuccess = 0;
            mesDataLogs.setRequestParams(JSON.toJSONString(dataFromPLC));
            mesDataLogs.setSuccess(requestSuccess);
            mesDataLogsMapper.insert(mesDataLogs);
            return null;
        }

        // 2.发送请求给MES
        String response = sendTireWeightToMES(dataFromPLC);

        if(!"".equals(response)){
            // 3.解析返回数据
            String result = "0";
            try {
                result = readResponse(response);
            } catch (Exception e) {
                log.error("MesUploadTireServiceImpl.uploadTireWeight() 解析MES响应数据失败！");
                e.printStackTrace();
                requestSuccess = 0;
            }finally {
                mesDataLogs.setRequestParams(JSON.toJSONString(dataFromPLC));
                mesDataLogs.setResponseData(JSON.toJSONString(result));
                mesDataLogs.setSuccess(requestSuccess);
                mesDataLogsMapper.insert(mesDataLogs);
            }

            // 4.下载请求结果到PLC
            plcContext.getPlcOperationApiService().writePlcByType("MES_Upload_Tire_Weight_Result", result, PlcDataType.INT_TYPE);

            return Integer.valueOf(result);
        }

        return 0;
    }


    /**
     * 重新上传输入的胎重
     * @param tireWeigh
     * @return
     */
    @Override
    public Res<?> reUploadTireWeight(String tireWeigh) {
        MesDataLogs mesDataLogs = new MesDataLogs();
        mesDataLogs.setInterfaceName("UpLoadTyreWeight");
        mesDataLogs.setCreateTime(new Date());
        Integer requestSuccess = 1;

        // 将胎重下发到PLC
        plcContext.getPlcOperationApiService().writePlcByType("MES_Tire_Weight", tireWeigh, PlcDataType.FLOAT_TYPE);

        // 1.获取PLC数据
        MesUploadTireDTO dataFromPLC = null;
        try {
            dataFromPLC = getDataFromPLC();
            if(!dataFromPLC.getGetAllDataSuccess()){
                requestSuccess = 0;
                mesDataLogs.setRequestParams(JSON.toJSONString(dataFromPLC));
                mesDataLogs.setSuccess(requestSuccess);
                mesDataLogsMapper.insert(mesDataLogs);
                return Res.fail("获取 当前计划 或 PLC数据 失败！！");
            }
        }catch (Exception e){
            log.error("MesUploadTireServiceImpl.uploadTireWeight(): 获取PLC数据失败！！！");
            e.printStackTrace();
            return Res.fail("获取 当前计划 或 PLC数据 失败！！");
        }
        dataFromPLC.setWeight(tireWeigh);

        // 2.发送请求给MES
        String response = sendTireWeightToMES(dataFromPLC);

        if(!"".equals(response)){
            // 3.解析返回数据
            String result = "0";
            try {
                result = readResponse(response);
            } catch (Exception e) {
                log.error("MesUploadTireServiceImpl.uploadTireWeight() 解析MES响应数据失败！");
                e.printStackTrace();

                return Res.fail("解析MES胎重结果数据失败！！");
            }finally {
                mesDataLogs.setRequestParams(JSON.toJSONString(dataFromPLC));
                mesDataLogs.setResponseData(JSON.toJSONString(result));
                mesDataLogs.setSuccess(requestSuccess);
                mesDataLogsMapper.insert(mesDataLogs);
            }

            // 4.下载请求结果到PLC
            plcContext.getPlcOperationApiService().writePlcByType("MES_Upload_Tire_Weight_Result", result, PlcDataType.INT_TYPE);
            HashMap<String, String> resultMap = new HashMap<>();
            resultMap.put("code",result);
            resultMap.put("msg","MES胎重结果：" + MES_TIRE_WEIGHT_RESULT.get(result));
            return Res.success(resultMap);
        }

        return Res.fail("请求MES胎重接口失败！！");
    }

    @Override
    public String getCurrentBarcode() {
        Object mes_bind_barCode = plcContext.getPlcOperationApiService().readPlcByType("MES_Bind_BarCode", PlcDataType.STRING_TYPE, (short) 1);
        if(mes_bind_barCode == null){
            log.error("MesUploadTireServiceImpl.getCurrentBarcode(): 没有从PLC获取到 mes_bind_barCode  标签的值");
            return "";
        }
        String mesBindBarcode = String.valueOf(mes_bind_barCode);
        return mesBindBarcode;
    }

    /**
     * 从PLC读取需要的上传胎重数据
     * @return
     */
    public MesUploadTireDTO getDataFromPLC(){
        MesUploadTireDTO mesUploadTireDTO = new MesUploadTireDTO();
        mesUploadTireDTO.setGetAllDataSuccess(false);

        // 1。机台号
        Object machine_pos = plcContext.getPlcOperationApiService().readPlcByType("Machine_Pos", PlcDataType.STRING_TYPE, (short) 1);
        if(machine_pos == null){
            log.error("MesUploadTireServiceImpl.getDataFromPLC(): 没有从PLC获取到 Machine_Pos  标签的值");
            return mesUploadTireDTO;
        }
        String machinePos = String.valueOf(machine_pos);
        mesUploadTireDTO.setEquipID(machinePos);

        // 2.条码
        Object mes_bind_barCode = plcContext.getPlcOperationApiService().readPlcByType("MES_Bind_BarCode", PlcDataType.STRING_TYPE, (short) 1);
        if(mes_bind_barCode == null){
            log.error("MesUploadTireServiceImpl.getDataFromPLC(): 没有从PLC获取到 mes_bind_barCode  标签的值");
            return mesUploadTireDTO;
        }
        String mesBindBarcode = String.valueOf(mes_bind_barCode);
        mesUploadTireDTO.setBarCode(mesBindBarcode);

        // 3.胎重
        Object mes_tire_weight = plcContext.getPlcOperationApiService().readPlcByType("MES_Tire_Weight", PlcDataType.FLOAT_TYPE, (short) 1);
        if(mes_tire_weight == null){
            log.error("MesUploadTireServiceImpl.getDataFromPLC(): 没有从PLC获取到 mes_tire_weight  标签的值");
            return mesUploadTireDTO;
        }
        String mesTireWeight = String.valueOf(mes_tire_weight);
        mesUploadTireDTO.setWeight(mesTireWeight);

        // 4.员工工号
        Object mes_user_num = plcContext.getPlcOperationApiService().readPlcByType("Mes_User_Num", PlcDataType.STRING_TYPE, (short) 1);
        if(mes_user_num == null){
            log.error("MesUploadTireServiceImpl.getDataFromPLC(): 没有从PLC获取到 mes_user_num  标签的值");
            return mesUploadTireDTO;
        }
        String mesUserNum = String.valueOf(mes_user_num);
        mesUploadTireDTO.setEmpID(mesUserNum);

        // 5.轮胎LITM号
        QueryWrapper<MesPlan> mesPlanQueryWrapper = new QueryWrapper<>();
        mesPlanQueryWrapper.eq("is_current_plan", 1);
        MesPlan mesPlanNow = mesPlanListMapper.selectOne(mesPlanQueryWrapper);
        if(mesPlanNow == null){
            log.error("MesUploadTireServiceImpl.getDataFromPLC(): 没有从数据库获取到 MesPlanNow  的值");
            return mesUploadTireDTO;
        }
        String litm = mesPlanNow.getLitm();
        mesUploadTireDTO.setMaterialID(litm);

        mesUploadTireDTO.setGetAllDataSuccess(true);


        return mesUploadTireDTO;

    }

    /**
     * 发送POST请求
     * @param dataFromPLC
     * @return
     */
    public String sendTireWeightToMES(MesUploadTireDTO dataFromPLC){
        QueryWrapper<MesInterfaceConfig> mesIpWrapper = new QueryWrapper<>();
        mesIpWrapper.eq("description","UpLoadTyreWeight");
        MesInterfaceConfig mesInterfaceConfig = this.mesInterfaceMapper.selectOne(mesIpWrapper);
        String mesUploadTireIp = mesInterfaceConfig.getIp();
        String mesUploadTirePath = mesInterfaceConfig.getPath();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("EquipID", dataFromPLC.getEquipID());
        params.add("BarCode", dataFromPLC.getBarCode());
        params.add("Weight", dataFromPLC.getWeight());
        params.add("MaterialID", dataFromPLC.getMaterialID());
        params.add("EmpID", dataFromPLC.getEmpID());

        String response = "";
        try {
            response = httpRequestService.sendPost(mesUploadTireIp, mesUploadTirePath, params);
        }catch (Exception e){
            log.error("MesUploadTireServiceImpl.uploadTireWeight(): 请求MES接口失败！！！");
            e.printStackTrace();
        }

        return response;
    }

    /**
     * 解析MES绑定条码后的结果
     * @param response
     * @return
     */
    public String readResponse(String response) throws Exception {
        String result = "0";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // 关键：开启命名空间支持
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(response.getBytes("UTF-8")));

        // 使用命名空间获取
        NodeList list = doc.getElementsByTagNameNS("http://tempuri.org/", "string");
        result = list.item(0).getTextContent();

        return result;
    }


}
