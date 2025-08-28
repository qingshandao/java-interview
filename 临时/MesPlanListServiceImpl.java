package com.tst.plc.mes.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.tst.plc.business.plan.mapper.MesPlanMapper;
import com.tst.plc.common.constants.PlcDataType;
import com.tst.plc.common.dto.Res;
import com.tst.plc.common.plc.service.impl.PlcContext;
import com.tst.plc.mes.dto.MesPlanDTO;
import com.tst.plc.mes.entity.MesDataLogs;
import com.tst.plc.mes.entity.MesInterfaceConfig;
import com.tst.plc.mes.entity.MesNewDataSet;
import com.tst.plc.mes.entity.MesPlan;
import com.tst.plc.mes.mapper.MesDataLogsMapper;
import com.tst.plc.mes.mapper.MesInterfaceMapper;
import com.tst.plc.mes.mapper.MesPlanListMapper;
import com.tst.plc.mes.service.IMesPlanListService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
@Slf4j
public class MesPlanListServiceImpl extends ServiceImpl<MesPlanListMapper, MesPlan> implements IMesPlanListService {
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


    @Override
    public Res<?> getMesPlan() {
        // 1.向MES计划接口发送请求
        // 1.1、获取MES接口地址
        QueryWrapper<MesInterfaceConfig> mesIpWrapper = new QueryWrapper<>();
        mesIpWrapper.eq("description","GetPlanDetail");
        MesInterfaceConfig mesInterfaceConfig = this.mesInterfaceMapper.selectOne(mesIpWrapper);
        String mesGetPlanIp = mesInterfaceConfig.getIp();
        String mesGetPlanPath = mesInterfaceConfig.getPath();

        // 1.2、获取机台号
        Object machine_pos = this.plcContext.getPlcOperationApiService().readPlcByType("Machine_Pos", PlcDataType.STRING_TYPE, (short) 1);
        if(machine_pos == null){
            log.error("MesPlanServiceImpl.getMesPlan(): 没有从PLC获取到Machine_Pos标签的值");
            return Res.fail("没有获取到成型机机台号！");
        }
        String machinePos = String.valueOf(machine_pos);

        // 1.3、获取班次
        Object banCi_name = this.plcContext.getPlcOperationApiService().readPlcByType("BanCi_Name", PlcDataType.INT_TYPE, (short) 1);
        if(banCi_name == null){
            log.error("MesPlanServiceImpl.getMesPlan(): 没有从PLC获取到BanCi_Name标签的值");
            return Res.fail("没有获取到班次！");
        }
        Integer banCiNum = Integer.valueOf(String.valueOf(banCi_name));
        String banCiName;
        switch (banCiNum) {
            case 1: banCiName = "早班"; break;
            case 2: banCiName = "中班"; break;
            case 3: banCiName = "夜班"; break;
            default: banCiName = "未知班次"; break;
        }

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("MACH_CODE", machinePos);
        params.add("BANCI_NAME", banCiName);

        String response = "";
        List<MesPlan> mesPlans = null;

        //记录日志
        // 日志请求参数
        Integer success = 1;
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("MACH_CODE", params.get("MACH_CODE"));
        requestMap.put("BANCI_NAME",params.get("BANCI_NAME"));
        String requestJson = JSON.toJSONString(requestMap);
        // 日志数据
        MesDataLogs mesDataLogs = new MesDataLogs();
        mesDataLogs.setInterfaceName("GetPlanDetail");
        mesDataLogs.setRequestParams(requestJson);
        try {
            response = httpRequestService.sendPost(mesGetPlanIp, mesGetPlanPath, params);
            // 2、解析响应数据
            mesPlans = readMesPlan(response);
        }catch (Exception e){
            success = 0;
            // 清空之前的计划
            QueryWrapper<MesPlan> mesPlanQueryWrapper = new QueryWrapper<>();
            mesPlanListMapper.delete(mesPlanQueryWrapper);
            return Res.fail("请求MES计划接口失败");
        }finally {
            // 存储响应数据
            mesDataLogs.setResponseData(JSON.toJSONString(mesPlans));
            mesDataLogs.setSuccess(success);
            mesDataLogs.setCreateTime(new Date());
            mesDataLogsMapper.insert(mesDataLogs);
        }

        if(mesPlans == null){
            // 清空之前的计划
            QueryWrapper<MesPlan> mesPlanQueryWrapper = new QueryWrapper<>();
            mesPlanListMapper.delete(mesPlanQueryWrapper);
            return Res.fail("MES计划为空！！");
        }

        // 3、存储响应数据
        boolean saveResult = savePlan(mesPlans);

        if(saveResult){
            return Res.success("请求MES获取计划成功！");
        }else {
            return Res.fail("解析MES计划响应数据失败");
        }
    }

    @Override
    public Res<?> changePlan(Integer id) {
        MesPlan updateEntity = new MesPlan();
        updateEntity.setIsCurrentPlan(0);
        mesPlanListMapper.update(
                updateEntity,
                new UpdateWrapper<MesPlan>().ne("id", id)
        );

        QueryWrapper<MesPlan> mesPlanQueryWrapper1 = new QueryWrapper<>();
        mesPlanQueryWrapper1.eq("id",id);
        MesPlan mesPlanNow = mesPlanListMapper.selectOne(mesPlanQueryWrapper1);
        mesPlanNow.setIsCurrentPlan(1);
        mesPlanListMapper.updateById(mesPlanNow);

        // 将生胎规格下载到PLC
        String desc1 = mesPlanNow.getDesc1();
        // 用正则匹配 4 位连续数字
        String mesPlanRecipe = "";
        Matcher matcher = Pattern.compile("\\d{4}").matcher(desc1);
        if (matcher.find()) {
            mesPlanRecipe = matcher.group();
        }


        this.plcContext.getPlcOperationApiService().writePlcByType("Mes_Plan_Recipe",mesPlanRecipe,PlcDataType.STRING_TYPE);


        return Res.success("切换计划成功");
    }

    /**
     * 解析请求MES计划接口后的响应数据
     * @param response
     * @return
     */
    public List<MesPlan> readMesPlan(String response){
        String raw = response; // 接口返回的 XML 字符串
        // 先取出 <string> 内的内容
        String escapedXml = raw.replaceAll("(?s)^.*?<string[^>]*>(.*)</string>.*$", "$1");
        // 解码转义字符：&lt; 变 <, &gt; 变 >
        String realXml = StringEscapeUtils.unescapeHtml4(escapedXml);

        // 反序列化
        XmlMapper  xmlMapper = new XmlMapper();
        MesNewDataSet newDataSet = null;
        try {
            newDataSet = xmlMapper.readValue(realXml, MesNewDataSet.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        // 转换为你的 MesPlan 实体
        List<MesPlan> mesPlans = null;
        if(newDataSet.getTables() != null){
            mesPlans = newDataSet.getTables().stream().map(t -> {
                MesPlan plan = new MesPlan();
                plan.setPlanId(t.getPlanId());   // 建议改成 String
                plan.setPlanCode(t.getPlanCode());
                plan.setPlanNum(t.getPlanNum());
                plan.setLitm(t.getLitm());
                plan.setDesc1(t.getDesc1());
                plan.setMorQuantity(t.getMorQuantity());
                plan.setMidQuantity(t.getMidQuantity());
                plan.setEveQuantity(t.getEveQuantity());
                plan.setCreateTime(new Date());
                plan.setUpdateTime(new Date());
                return plan;
            }).collect(Collectors.toList());
        }
        if(mesPlans != null){
            savePlan(mesPlans);
        }

        return mesPlans;
    }

    /**
     * 存储解析后的MES计划数据
     * @param mesPlans
     * @return
     */
    public boolean savePlan(List<MesPlan> mesPlans){
        ArrayList<Integer> idsInMes = new ArrayList<>();
        try {
            for (MesPlan mesPlan : mesPlans) {
                QueryWrapper<MesPlan> mesPlanQueryWrapper = new QueryWrapper<>();
                mesPlanQueryWrapper.eq("plan_id", mesPlan.getPlanId());
                MesPlan mesPlanInSQL = mesPlanListMapper.selectOne(mesPlanQueryWrapper);

                if(mesPlanInSQL != null){
                    mesPlan.setId(mesPlanInSQL.getId());
                    mesPlan.setCreateTime(mesPlanInSQL.getCreateTime());
                    mesPlan.setIsCurrentPlan(mesPlanInSQL.getIsCurrentPlan());
                    mesPlanListMapper.updateById(mesPlan);
                    idsInMes.add(mesPlanInSQL.getId());
                }else{
                    mesPlanListMapper.insert(mesPlan);
                    idsInMes.add(mesPlan.getId()); // 插入后自动回填主键ID
                }
            }

            // 删除之前保存的MES计划
            deleteOldMesPlan(idsInMes);
        }catch (Exception e){
            log.error("MesPlanServiceImpl.savePlan(): 存储MES计划数据失败！！");
            log.error(mesPlans.toString());
            e.printStackTrace();
            return false;
        }


        return true;
    }

    /**
     * 删除之前保存过的配方
     * @param idsInMes
     * @return
     */
    public boolean deleteOldMesPlan(List<Integer> idsInMes){
        QueryWrapper<MesPlan> mesPlanQueryWrapper = new QueryWrapper<>();
        mesPlanQueryWrapper.notIn("id", idsInMes);
        mesPlanListMapper.delete(mesPlanQueryWrapper);
        return true;
    }
}
