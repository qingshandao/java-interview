# 零、流程概览



![](./assets/%E6%B5%81%E7%A8%8B%E5%9B%BE.png)

# 一、用户登录

## 0、概述

MES登录独立于HMI登录逻辑，MES用户登录仅为了获取用户的用户名。由SE请求Web，登录MES。

![](./assets/MES%E7%94%A8%E6%88%B7%E7%99%BB%E5%BD%95%E9%A1%B5.png)

## 1、Web 接口设计

### 1.1、MES用户登录接口

- Path：`/mesLogin/login`

- Method：`post`

- Data：

  | 参数名称 |  类型  |    备注    |
  | :------: | :----: | :--------: |
  |   USER   | String | 用户的工号 |
  | Password | String | 用户的密码 |

- 返回值：

  | 参数名称 |  类型  |                             备注                             |
  | :------: | :----: | :----------------------------------------------------------: |
  | nickName | String | 请求MES 登录服务之后的结果（“0”：登陆失败；“用户名”：登陆成功） |

- 接口文件：`com.tst.plc.mes.controller.MesLoginController.java`

## 2、Web 表设计

无需数据库表。

## 3、Web 请求MES用户登录逻辑

### 3.1、逻辑概述

将接收到的工号和密码发送给MES登录接口，解析响应数据，并返回响应结果

### 3.2、MES登录接口（RestTemplate后端请求）

- Path：http://10.135.55.206/MES_TBM1.asmx/CXDoLogin

- Method：`POST`

- Content-Type：`application/x-www-form-urlencoded` 

- Data：

  | 参数名称 |  类型  |    备注    |
  | :------: | :----: | :--------: |
  |   USER   | String | 用户的工号 |
  | Password | String | 用户的密码 |

  > 测试数据：
  >
  > - USER = ”HL2094“
  > - Password = ”000000“

- 返回数据

  | 参数名称 |  类型  |                       备注                        |
  | :------: | :----: | :-----------------------------------------------: |
  |    /     | String | MES 登录结果（“0”：登陆失败；“用户名”：登陆成功） |
  
  > 查询结果示例：
  >
  > 成功：
  >
  > ```xml
  > <?xml version="1.0" encoding="utf-8"?>
  > <string xmlns="DCHL">查星星</string>
  > ```
  >
  > 失败：
  >
  > ```xml
  > <?xml version="1.0" encoding="utf-8"?>
  > <string xmlns="DCHL">0</string>
  > ```
  >
  > 



# 二、计划信息

## 0、概述

在 计划信息 页中，请求MES计划接口，获取到计划数据后，存储到工控机数据库中，最终显示到 HMI-Web 前端页面。

页面中显示所有的订单以及订单的所有信息，用户通过点击”接受“按钮，切换不同的计划。

![](./assets/MES%E8%AE%A2%E5%8D%95%E9%A1%B5%E9%9D%A2.png)

## 1、Web 接口设计

### 1.1、查询计划信息接口

- 接口位置：`com.tst.plc.mes.controller.MesPlanController.java`

- Path：`/mesPlan/list`

- Method：`get`

- Query 请求参数

  | 参数名称 | 参数类型 | 是否必须 |     备注     |
  | :------: | :------: | :------: | :----------: |
  | current  |   int    |    是    |   当前页码   |
  |   size   |   int    |    是    | 查询数据条数 |

- 返回值

  | 名称                                |        类型         |  是否必须  |                 备注                 |
  | :---------------------------------- | :-----------------: | :--------: | :----------------------------------: |
  | res                                 |       Object        |     是     |               返回信息               |
  | \|— data<br />        \|—data.total | Object<br /> number | 是<br />是 |                                      |
  | \|— data.records                    |      object[]       |     是     |            查询到的计划表            |
  | \|— isCurrentPlan                   |         int         |     是     | 是否是当前使用中的计划（0=否；1=是） |
  | \|— planId                          |       number        |     是     |         从MES获取到的计划ID          |
  | \|— planCode                        |       string        |     是     |               计划代码               |
  | \|— litmCode                        |       string        |     是     |         8位LITM（生胎代码）          |
  | \|— litmDsc                         |       string        |     是     |     从MES获取的DSC1（生胎规格）      |
  | \|— planNum                         |       number        |     是     |               计划产量               |
  | \|— morQuantity                     |       number        |     是     |             早班完工产量             |
  | \|— midQuantity                     |       number        |     是     |             中班完工产量             |
  | \|— eveQuantity                     |       number        |     是     |             夜班完工产量             |

## 2、Web 表设计

从 MES 接口获取到的计划订单数据，存储在 `mes_order_list`  表：

| **字段名**      | **数据类型** | **说明**               | 是否必须 | **备注**     |
| --------------- | ------------ | ---------------------- | :------: | ------------ |
| id              | int          | 主键                   |    ✅     | 自增         |
| plan_id         | varchar(50)  | mes计划id              |    ✅     |              |
| plan_code       | varchar(50)  | 计划代码               |    ✅     |              |
| plan_num        | int          | 计划产量               |    ✅     |              |
| litm            | varchar(50)  | 生胎规格               |    ✅     |              |
| desc1           | varchar(50)  | 生胎规格中文描述       |    ✅     | 配方规格描述 |
| ⚠ mor_quantity  | int          | 早班完工               |    ✅     |              |
| ⚠ mid_quantity  | int          | 中班完工               |    ✅     |              |
| ⚠ eve_quantity  | int          | 夜班完工               |    ✅     |              |
| create_time     | datetime     | 创建日期               |    ✅     |              |
| update_time     | datetime     | 更新时间               |    ✅     |              |
| is_current_plan | int          | 是否为当前使用中的计划 |    ✅     | 0=否；1=是   |

## 3、MES 获取计划订单业务

### 3.1、MES接口（RestTemplate后端请求）

- Path：http://10.135.55.206/MES_TBM1.asmx/GetPlanDetail

- Method：`POST`

- Content-Type：`application/x-www-form-urlencoded`

- Data：

  |  参数名称  |  类型  |            备注            |
  | :--------: | :----: | :------------------------: |
  | MACH_CODE  | String |         成型机编号         |
  | BANCI_NAME | String | 班次名（早班，中班，夜班） |

  > 测试数据：
  >
  > - MACH_CODE= ”L502“
  > - BANCI_NAME= ”早班“

- 返回数据

  |   参数名称    |  类型  |       备注       |
  | :-----------: | :----: | :--------------: |
  |    PLAN_ID    | string |    生胎计划ID    |
  |     LITM      | string | 生胎代码（8位）  |
  |     DSC1      | string | 生胎规格中文描述 |
  | MOR_QUANTITY  | string |     早班完工     |
  | MID_QUANTITY  | string |     中班完工     |
  |  EVE_QUANTIT  | string |     夜班完工     |
  |   PLAN_CODE   | string | 需确认从哪里获取 |
  | PLAN_QUANTITY | string |     计划产量     |
  
  > 返回参数示例：
  >
  > ```xml
  > <?xml version="1.0" encoding="utf-8"?>
  > <string xmlns="DCHL">&lt;NewDataSet&gt;
  >   &lt;Table&gt;
  >     &lt;PLAN_ID&gt;YC250819L503250211&lt;/PLAN_ID&gt;
  >     &lt;LITM&gt;80527872&lt;/LITM&gt;
  >     &lt;DSC1&gt;生胎 2502&lt;/DSC1&gt;
  >     &lt;MOR_QUANTITY&gt;87&lt;/MOR_QUANTITY&gt;
  >     &lt;MID_QUANTITY&gt;0&lt;/MID_QUANTITY&gt;
  >     &lt;EVE_QUANTITY&gt;0&lt;/EVE_QUANTITY&gt;
  >     &lt;PLAN_CODE&gt;YC250819&lt;/PLAN_CODE&gt;
  >     &lt;PLAN_QUANTITY&gt;100&lt;/PLAN_QUANTITY&gt;
  >   &lt;/Table&gt;
  >   &lt;Table&gt;
  >     &lt;PLAN_ID&gt;YC250819L503399612&lt;/PLAN_ID&gt;
  >     &lt;LITM&gt;80641351&lt;/LITM&gt;
  >     &lt;DSC1&gt;生胎 3996&lt;/DSC1&gt;
  >     &lt;MOR_QUANTITY&gt;0&lt;/MOR_QUANTITY&gt;
  >     &lt;MID_QUANTITY&gt;0&lt;/MID_QUANTITY&gt;
  >     &lt;EVE_QUANTITY&gt;0&lt;/EVE_QUANTITY&gt;
  >     &lt;PLAN_CODE&gt;YC250819&lt;/PLAN_CODE&gt;
  >     &lt;PLAN_QUANTITY&gt;8&lt;/PLAN_QUANTITY&gt;
  >   &lt;/Table&gt;
  > &lt;/NewDataSet&gt;</string>
  > ```
  >
  > 没有计划时：
  >
  > ```xml
  > <?xml version="1.0" encoding="utf-8"?>
  > <string  xmlns="DCHL">&lt;NewDataSet /&gt;</string>
  > ```
  >
  > 测试计划：
  >
  > ```xml
  > <?xml version="1.0" encoding="utf-8"?>
  > <string  xmlns="DCHL">&lt;NewDataSet&gt;  &lt;Table&gt;    &lt;PLAN_ID&gt;YC250822L502280011&lt;/PLAN_ID&gt;    &lt;LITM&gt;80527579&lt;/LITM&gt;    &lt;DSC1&gt;生胎 2800&lt;/DSC1&gt;    &lt;MOR_QUANTITY&gt;0&lt;/MOR_QUANTITY&gt;    &lt;MID_QUANTITY&gt;0&lt;/MID_QUANTITY&gt;    &lt;EVE_QUANTITY&gt;0&lt;/EVE_QUANTITY&gt;    &lt;PLAN_CODE&gt;YC250822&lt;/PLAN_CODE&gt;    &lt;PLAN_QUANTITY&gt;0&lt;/PLAN_QUANTITY&gt;  &lt;/Table&gt;  &lt;Table&gt;    &lt;PLAN_ID&gt;YC250822L502280012&lt;/PLAN_ID&gt;    &lt;LITM&gt;80527579&lt;/LITM&gt;    &lt;DSC1&gt;生胎 2800&lt;/DSC1&gt;    &lt;MOR_QUANTITY&gt;0&lt;/MOR_QUANTITY&gt;    &lt;MID_QUANTITY&gt;0&lt;/MID_QUANTITY&gt;    &lt;EVE_QUANTITY&gt;0&lt;/EVE_QUANTITY&gt;    &lt;PLAN_CODE&gt;YC250822&lt;/PLAN_CODE&gt;    &lt;PLAN_QUANTITY&gt;0&lt;/PLAN_QUANTITY&gt;  &lt;/Table&gt;&lt;/NewDataSet&gt;</string>
  > ```





# 三、胎胚条码绑定

## 0、概述

条码绑定的调用分为PLC自动触发和手动重新绑定条码

- PLC自动触发：PLC给Web发送胎胚信息读取完毕信号，Web向PLC先发送等待MES结果信号，再向MES接口发送条码绑定请求，将绑定结果最后下发给PLC；如果绑定成功，则自动调用后续胎重上传的逻辑，如果绑定失败，则不调用胎重上传逻辑。

- 手动重新绑定条码：页面中手动输入新条码，向MES发送绑定请求，将绑定结果提示在页面上。

  ![](./assets/MES%E9%87%8D%E6%96%B0%E7%BB%91%E5%AE%9A%E6%9D%A1%E7%A0%81%E9%A1%B5%E9%9D%A2.png)

  > ⚠注：手动重新绑定条码的逻辑，无论绑定是否成功，都不调用后续的胎重上传逻辑。

## 1、Web 接口设计

- PLC 自动触发：不需要接口，在PLC发送信号给HMI的方法中（`com.tst.plc.business.tireUnload.service.impl. ProductionOutputServiceImpl.java`），调用绑定条码的Service即可；
- 手动重新绑定条码：提供重新修改PLC里待绑定条码标签并请求MES条码绑定的接口和服务。

### 1.1、重新绑定条码接口

- 接口位置：`com.tst.plc.mes.controller.MesPlanController.java`

- Path：`/mesPlan/reBindBarCode`

- Method：`post`

- Query 请求参数

  | 参数名称 | 参数类型 | 是否必须 |     备注     |
  | :------: | :------: | :------: | :----------: |
  | barCode  |  string  |    是    | 待绑定新条码 |
  
- 返回值

  | 名称          |  类型  | 是否必须 |        备注        |
  | :------------ | :----: | :------: | :----------------: |
  | res           | Object |    是    |      返回信息      |
  | \|— data      | Object |    是    |                    |
  | \|— data.code | string |    是    | 重新绑定后的结果值 |

## 2、Web数据库表设计

无需数据库表结构

## 3、MES 绑定条码业务

### 3.1、MES绑定条码接口（RestTemplate后端请求）

- Path：http://10.135.55.206/MES_TBM1.asmx/setWF_02

- Method：`POST`

- Data：

  |  参数名称  |  类型  |             备注             |
  | :--------: | :----: | :--------------------------: |
  | MACH_CODE  | String |          成型机编号          |
  |  BC_CODE   | String |           胎胚条码           |
  | LITM_CODE  | String |           8位LITM            |
  | ACCT_CODE  | String |           员工工号           |
  | BANCI_NAME | String |  班次名（早班，中班，夜班）  |
  |  PLAN_ID   | String | GetPlanDetail中获取的PLAN_ID |
  |  MFG_DATE  |  Date  |           操作日期           |

  > 测试数据：
  >
  > - MACH_CODE = ”L502“
  > - BC_CODE = ”1234567890“
  > - LITM_CODE = ”80527579“
  > - ACCT_CODE = ”HL2094“
  > - BANCI_NAME = ”早班“
  > - PLAN_ID = "YC250825L502280011"
  > - MFG_DATE = ”2025-08-26 16:32:26“

- 返回数据

  | 参数名称 | 类型 |           备注           |
  | :------: | :--: | :----------------------: |
  |    /     | int  | 绑定条码后的判断返回结果 |
  
  > 返回内容：
  >
  > ```xml
  > <?xml version="1.0" encoding="utf-8"?>
  > <int xmlns="DCHL">0</int>
  > ```



# 四、胎胚重量重新上传

## 0、概述

胎胚重量重新上传的调用分为PLC自动触发和手动重新上传重量

- PLC自动触发：PLC给Web发送胎胚信息读取完毕信号，Web向PLC先发送等待MES结果信号，判断条码绑定是否成功，如果成功，则再向MES接口发送条码绑定请求，将绑定结果最后下发给PLC；如果绑定成功，则自动调用后续胎重上传的逻辑，如果绑定失败，则不调用胎重上传逻辑。

- 手动重新绑定条码：页面中手动输入新条码，向MES发送绑定请求，将绑定结果提示在页面上。

  ![](./assets/MES%E9%87%8D%E6%96%B0%E4%B8%8A%E4%BC%A0%E8%83%8E%E9%87%8D%E9%A1%B5%E9%9D%A2.png)

  

## 1、Web 接口设计

- PLC 自动触发：不需要接口，在PLC发送信号给HMI的方法中（`com.tst.plc.business.tireUnload.service.impl. ProductionOutputServiceImpl.java`），调用上传胎重的Service即可；
- 手动重新上传胎重：提供修改PLC里”待重新上传胎重“标签并请求MES胎重上传的接口和服务。

### 1.1、重新上传胎重接口

- 接口位置：`com.tst.plc.mes.controller.MesPlanController.java`

- Path：`/mesPlan/reUploadTireWeight`

- Method：`post`

- Query 请求参数

  | 参数名称  | 参数类型 | 是否必须 |      备注      |
  | :-------: | :------: | :------: | :------------: |
  | tireWeigh |  string  |    是    | 待上传的新胎重 |

- 返回值

  | 名称          |  类型  | 是否必须 |          备注          |
  | :------------ | :----: | :------: | :--------------------: |
  | res           | Object |    是    |        返回信息        |
  | \|— data      | Object |    是    |                        |
  | \|— data.code | string |    是    | 重新上传胎重后的结果值 |

## 2、Web数据库表设计

无需数据库表结构

## 3、MES 上传胎重业务

### 3.1、MES绑定条码接口（RestTemplate后端请求）

- Path：http://128.141.55.212/ClientDataService/ClientDataService.asmx/UpLoadTyreWeight

- Method：`POST`

- Data：

  |  参数名称  |  类型  |        备注         |
  | :--------: | :----: | :-----------------: |
  |  EquipID   | String |   设备/成型机编号   |
  |  BarCode   | String |      轮胎条码       |
  |   Weight   | String |      轮胎重量       |
  | MaterialID | String | 8位LITM轮胎规格代号 |
  |   EmpID    | String |      员工编号       |

  > 测试数据：
  >
  > - EquipID= ”L502“
  > - BarCode= ”1234567890“
  > - Weight= ”10.52“
  > - MaterialID= ”80527579“
  > - EmpID= ”HL2094“

- 返回数据

  | 参数名称 |  类型  |           备注           |
  | :------: | :----: | :----------------------: |
  |    /     | string | 绑定条码后的判断返回结果 |

  > 返回内容示例：
  >
  > ```xml
  > <?xml version="1.0" encoding="utf-8"?>
  > <string xmlns="http://tempuri.org/">2</string>
  > ```



# 五、MES日志记录

## 0、概述

记录每次向MES不同的接口发送请求的数据和MES返回的数据，请求的MES接口包括：

- MES用户登录接口
- MES计划获取接口
- MES条码绑定接口
- MES胎重上传接口

记录日志内容样例如下：

- MES登录接口

  ![](./assets/MES%E4%BC%A0%E8%BE%93%E6%97%A5%E5%BF%97%E9%A1%B5%E9%9D%A2_%E7%99%BB%E5%BD%95%E6%97%A5%E5%BF%97.png)

- MES计划获取接口

  ![](./assets/MES%E4%BC%A0%E8%BE%93%E6%97%A5%E5%BF%97%E9%A1%B5%E9%9D%A2_%E8%AE%A1%E5%88%92%E6%97%A5%E5%BF%97.png)

- MES条码绑定接口

  ![](./assets/MES%E4%BC%A0%E8%BE%93%E6%97%A5%E5%BF%97%E9%A1%B5%E9%9D%A2_%E6%9D%A1%E7%A0%81%E7%BB%91%E5%AE%9A%E6%97%A5%E5%BF%97.png)

- MES胎重上传接口

  ![](./assets/MES%E4%BC%A0%E8%BE%93%E6%97%A5%E5%BF%97%E9%A1%B5%E9%9D%A2_%E8%83%8E%E9%87%8D%E7%BB%91%E5%AE%9A%E6%97%A5%E5%BF%97.png)



## 1、Web 接口设计

- PLC 自动触发：不需要接口，在PLC发送信号给HMI的方法中（`com.tst.plc.business.tireUnload.service.impl. ProductionOutputServiceImpl.java`），调用上传胎重的Service即可；
- 手动重新上传胎重：提供修改PLC里”待重新上传胎重“标签并请求MES胎重上传的接口和服务。

### 1.1、重新上传胎重接口

- 接口位置：`com.tst.plc.mes.controller.MesPlanController.java`

- Path：`/mesPlan/reUploadTireWeight`

- Method：`post`

- Query 请求参数

  | 参数名称  | 参数类型 | 是否必须 |      备注      |
  | :-------: | :------: | :------: | :------------: |
  | tireWeigh |  string  |    是    | 待上传的新胎重 |

- 返回值

  | 名称          |  类型  | 是否必须 |          备注          |
  | :------------ | :----: | :------: | :--------------------: |
  | res           | Object |    是    |        返回信息        |
  | \|— data      | Object |    是    |                        |
  | \|— data.code | string |    是    | 重新上传胎重后的结果值 |

## 2、Web数据库表设计

无需数据库表结构



# 六、MES接口配置

## 0、概述

MES接口的地址可能会更换，因此将MES请求接口配置在数据库中保存

## 1、Web数据库表设计

存储MES接口的配置在 `t_mes_interface_list`

| **字段名**  | **数据类型** | **说明** | 是否必须 | **备注**      |
| ----------- | ------------ | -------- | :------: | ------------- |
| id          | int          | 主键     |    ✅     | 自增          |
| ip          | string       | 条码     |    ✅     | MES接口IP地址 |
| path        | float        | 胎重     |    ✅     | MES接口路径   |
| desc        | varchar(50)  | 接口描述 |    ✅     | 接口描述      |
| update_time | datetime     | 记录时间 |    ✅     | 更新时间      |



# 七、PLC标签准备

| **字段名**                    | **数据类型** | **说明**            | **备注**                                                     |
| ----------------------------- | ------------ | ------------------- | ------------------------------------------------------------ |
| Mes_User_Num                  | String       | MES员工工号         |                                                              |
| BanCi_Name                    | DINT         | MES班次号           | 1=早班；2=中班；3=夜班                                       |
| Mes_Plan_Recipe               | String       | MES生胎规格         | 例如：“生胎2096”                                             |
| MES_Bind_Barcode_Result       | DINT         | MES绑定条码返回结果 | 0=绑定失败；1=绑定成功；2=未找到条码；3=条码已绑定；4=未找到机台；5=未找到班次 |
| MES_Bind_BarCode              | String       | MES待绑定条码       | 更新时间                                                     |
| MES_Upload_Tire_Weight_Result | DINT         | MES上传胎重返回结果 | 0=网络异常；1=合格；2=轻；3=重；4=条码异常(码长为10)；5=物料号异常(长度为8)；6=条码不存在；7=标准重量查询失败；8 = `SetBCWeight_Mes` 接口调用失败 |
| MES_Tire_Weight               | REAL         | MES待上传胎重       |                                                              |



# 八、待办

- [ ] SE 嵌入的 URL 文档
