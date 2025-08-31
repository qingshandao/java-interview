package com.tst.plc.mes.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.tst.plc.common.dto.Res;
import com.tst.plc.mes.dto.MesDataLogsDTO;
import com.tst.plc.mes.entity.MesDataLogs;
import com.tst.plc.mes.entity.MesPlan;
import com.tst.plc.mes.service.IMesDataLogsService;
import com.tst.plc.mes.service.IMesPlanListService;
import com.tst.plc.mes.service.IMesSetBarCode;
import com.tst.plc.mes.service.IMesUploadTireService;
import com.tst.plc.utils.Func;
import com.tst.plc.utils.MessageUtils;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/mesPlan")
public class MesPlanController {

    @Resource
    private IMesPlanListService mesPlanListService;

    @Resource
    private IMesSetBarCode mesSetBarCode;

    @Resource
    private IMesUploadTireService mesUploadTireService;

    @Resource
    private IMesDataLogsService mesDataLogsService;

    @ApiOperation(value = "查询mes计划列表", notes = "查询mes计划列表")
    @GetMapping("/list")
    public Res list(Page<MesPlan> page) throws Throwable {
        QueryWrapper<MesPlan> wrapper = new QueryWrapper<>();

        wrapper.orderByDesc("create_time");
        Page<MesPlan> result = mesPlanListService.page(page, wrapper);
        return Res.success(MessageUtils.message("mesPlan.list.success"), result);
    }


    @ApiOperation(value = "请求mes，刷新mes计划列表", notes = "请求mes，刷新mes计划列表")
    @GetMapping("/refresh")
    public Res refreshList() throws Throwable {
        Res<?> mesPlanResult = mesPlanListService.getMesPlan();

        return mesPlanResult;
    }

    @ApiOperation(value = "切换MES计划", notes = "切换MES计划")
    @GetMapping("/changePlan")
    public Res changePlan(@RequestParam("id") Integer id) throws Throwable {
        Res<?> mesPlanResult = mesPlanListService.changePlan(id);

        return mesPlanResult;
    }

    @ApiOperation(value = "重新绑定MES条码", notes = "重新绑定MES条码")
    @PostMapping("/reBindBarCode")
    public Res reBindBarCode(@RequestParam("barCode") String barCode){

        Res<?> res = mesSetBarCode.reBindBarCode(barCode);

        return res;
    }

    @ApiOperation(value = "重新上传胎重", notes = "重新上传胎重")
    @PostMapping("/reUploadTireWeight")
    public Res reUploadTireWeight(@RequestParam("tireWeigh") String tireWeigh){

        Res<?> res = mesUploadTireService.reUploadTireWeight(tireWeigh);

        return res;
    }

    /**
     * 查询MES的不同接口的日志记录
     * @param type：接口类型
     * @return
     */
    @ApiOperation(value = "查询MES日志记录", notes = "查询MES日志记录")
    @PostMapping("/logs")
    public Res<?> list(@RequestParam("current") Long current, @RequestParam("size")Long size,  @RequestParam("type") String type){
        if(!"".equals(type) && type != null){
            Page<MesDataLogs> page = new Page<>();
            page.setCurrent(Long.valueOf(current));
            page.setSize(Long.valueOf(size));

            QueryWrapper<MesDataLogs> wrapper = new QueryWrapper<>();
            wrapper.eq("interface_name", type);
            wrapper.orderByDesc("create_time");

            Page<MesDataLogs> result = mesDataLogsService.page(page, wrapper);

            Page<MesDataLogsDTO> dtoPage = new Page<>(page.getCurrent(), page.getSize(), result.getTotal());

            List<MesDataLogsDTO> dtoList = result.getRecords().stream().map(log -> {
                MesDataLogsDTO dto = new MesDataLogsDTO();
                dto.setId(log.getId());
                dto.setInterfaceName(log.getInterfaceName());
                dto.setRequestParams(JSON.parseObject(log.getRequestParams(), new TypeReference<Map<String, Object>>(){}));
                if("CXDoLogin".equals(type)){
                    dto.setResponseData(JSON.parseObject(log.getResponseData(), new TypeReference<Map<String, Object>>(){}));
                }else if("GetPlanDetail".equals(type)){
                    dto.setResponseData(extractPlanIds(log.getResponseData()));
                }else if("setWF_02".equals(type) || "UpLoadTyreWeight".equals(type)){
                    HashMap<String, Object> jsonMap = new HashMap<>();
                    jsonMap.put("result", JSON.parseObject(log.getResponseData(), String.class));
                    dto.setResponseData(jsonMap);
                }

                dto.setSuccess(log.getSuccess());
                dto.setCreateTime(log.getCreateTime());
                return dto;
            }).collect(Collectors.toList());

            dtoPage.setRecords(dtoList);

            return Res.success(dtoPage);
        }

        return Res.fail("未指定日志类型！");
    }

    /**
     * 查询当前PLC中的待绑定条码
     * @return
     */
    @ApiOperation(value = "查询当前使用的PLC条码", notes = "查询当前使用的PLC条码")
    @GetMapping("/getCurrentBarcode")
    public Res<?> getCurrentBarcode(){
        String currentBarcode = mesUploadTireService.getCurrentBarcode();

        if(!"".equals(currentBarcode)){
            return Res.success("获取条码成功", (Object) currentBarcode);
        }else{
            return Res.fail("获取胎重绑定的条码失败");
        }
    }

    /**
     * 将MES计划的JSON响应数据，解析出所有的计划ID
     * @param jsonStr
     * @return
     */
    private Map<String, Object> extractPlanIds(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty() || "null".equals(jsonStr)) {
            return null;
        }
        try {
            Object obj = JSON.parse(jsonStr);
            List<String> planIds = new ArrayList<>();

            if (obj instanceof JSONObject) {
                // JSON 对象
                collectPlanIds((JSONObject) obj, planIds);
            } else if (obj instanceof JSONArray) {
                // JSON 数组
                collectPlanIds((JSONArray) obj, planIds);
            }

            if (planIds.isEmpty()) {
                return null;
            }
            Map<String, Object> result = new HashMap<>();
            result.put("Plan_IDs", String.valueOf(planIds));
            return result;

        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void collectPlanIds(JSONObject jsonObj, List<String> planIds) {
        for (Map.Entry<String, Object> entry : jsonObj.entrySet()) {
            if ("planId".equalsIgnoreCase(entry.getKey())) {
                planIds.add(String.valueOf(entry.getValue()));
            } else if (entry.getValue() instanceof JSONObject) {
                collectPlanIds((JSONObject) entry.getValue(), planIds);
            } else if (entry.getValue() instanceof JSONArray) {
                collectPlanIds((JSONArray) entry.getValue(), planIds);
            }
        }
    }

    private void collectPlanIds(JSONArray jsonArr, List<String> planIds) {
        for (Object element : jsonArr) {
            if (element instanceof JSONObject) {
                collectPlanIds((JSONObject) element, planIds);
            } else if (element instanceof JSONArray) {
                collectPlanIds((JSONArray) element, planIds);
            }
        }
    }
}
