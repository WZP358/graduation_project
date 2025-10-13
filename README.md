# 基于信号处理的音频节拍自动识别平台

## 背景

​	音乐节拍是音乐结构中最基础的组成部分，它构成了音乐的骨架和律动感。**音频节拍自动识别（Beat Tracking）** 作为音乐信息检索领域的核心课题之一，旨在让计算机能够像人类乐手或听众一样，实时地、准确地感知并定位音频信号中的节拍点。这项技术对于音乐分析、自动伴奏系统、音乐教育、舞蹈游戏、健身软件（如跑步节奏匹配）以及DJ辅助打碟等众多领域都具有极其重要的应用价值。

​	传统的节拍识别方法主要基于**信号处理** 技术，例如通过计算短时能量、频谱通量（Spectral Flux）、自相关函数等特征来检测信号的周期性脉冲。这些方法在节奏规整的流行音乐或电子音乐上表现尚可，但其局限性也十分明显：它们严重依赖于预设的阈值和启发式规则，对于节奏复杂、速度多变（如 rubato）、或打击乐不明显的音乐类型（如古典音乐、爵士乐），其识别准确率会显著下降，缺乏足够的鲁棒性。

​	近年来，随着**深度学习** 技术的飞速发展，尤其是卷积神经网络（CNN）和循环神经网络（RNN）在时序信号处理上的成功应用，基于数据驱动的节拍识别方法取得了突破性进展。这些模型能够从海量音频数据中自动学习高层次、抽象的特征表示，从而更有效地建模音乐中的时序依赖关系，在处理复杂音乐场景时展现出远超传统方法的潜力。

​	尽管底层算法研究已硕果累累，但将其转化为用户可用的实用工具仍然存在缺口。当前主流的大型音乐可视化与编辑平台（如 Audacity、Sonic Visualiser 等）虽然功能强大，但其设计重心在于通用音频编辑和基础分析，操作逻辑相对复杂，且普遍缺乏集成的、端到端的节拍自动识别功能。用户往往需要手动标注节拍点，这个过程不仅枯燥耗时，而且结果带有极强的主观性，难以保证一致性和准确性。

​	因此，本项目旨在开发一个**专门化的音频节拍自动识别平台**。该平台将致力于融合传统信号处理方法的可解释性与深度学习模型的高精度优势，提供一个集**音频数据可视化**、**自动节拍识别**、**节拍数据管理**及**结果对比验证**于一体的完整解决方案。最终目标是降低这项技术的使用门槛，为音乐研究者、开发者和爱好者提供一个高效、可靠且用户友好的工具，以推动节拍识别技术从学术研究走向实际应用。



## 核心问题

1. **手动标记的局限性、低效性与数据一致性问题**
    - **耗时费力**：对于长达数分钟的音乐，用户必须全程高度集中，跟随播放进行敲击标记，这个过程极其枯燥且容易疲劳。
    - **主观性强**：不同用户对节拍的感知存在差异（例如，有人感知军鼓点，有人感知底鼓点），甚至同一用户在不同时间段的标记结果也可能前后不一致。这导致收集到的节拍数据缺乏客观标准，难以用于算法评估或模型训练。
    - **精度有限**：人手的反应速度与音频播放之间存在不可避免的延迟，导致标记的时间点存在系统误差，难以达到毫秒级的精度要求。
2. **节拍数据的存储与管理**：需要设计合理的数据结构来存储节拍时刻数据，并实现高效的数据存取功能。
3. **自动识别算法的准确性**：如何选择合适的信号处理算法，实现高精度的节拍自动识别是一个技术挑战。
4. **可视化展示的完整性**：需要将自动识别的节拍点与原始波形图有机结合，提供直观的可视化效果。
5. **算法比较：**实现不同方法得到的“节拍时刻”数据在同一波形图进行对比。
6. **自适应学习能力**：更进一步的，我们能不能基于机器学习实现实时节拍识别功能？比如在音频播放过程中，根据已播放部分的节拍模式，实时预测下一个节拍点即将出现的时间并在图像上波形图上标记出来？预计错误是很正常的，那我们能不能总结错误，以提高下次预测的精准度？

## 对于解决核心问题的初步想法

1.  **模块化与微服务架构：实现高内聚、低耦合的系统设计**
    - **前端展示层 (Vue.js)**：专注于用户体验和交互，负责波形图、频谱图、节拍标记、控制栏等所有可视化元素的渲染与用户交互事件的处理。
    - **业务逻辑层 (Spring Boot)**：处理核心业务，如用户请求路由、项目与音频文件管理、数据库的CRUD操作（创建、读取、更新、删除）。它作为中控，协调前后端与计算服务。
    - **计算服务层 (Python Flask)**：专门负责所有音频分析和算法计算任务，例如使用`librosa`和`madmom`进行节拍跟踪、特征提取。通过RESTful API与业务逻辑层通信，实现技术栈异构（Java+Python）和优势互补。
    - **好处**：该架构使系统易于维护、扩展和独立部署。例如，可以单独优化计算服务层的性能，或替换新的节拍识别算法，而无需改动其他模块。
2.  **混合算法策略：**平台将同时集成**传统方法**（如基于能量包络的峰值检测）和**现代深度学习方法**（如基于CRNN的节拍跟踪）。用户可以选择使用哪种算法，并可设置相关参数。
3.  **数据库设计优化**：设计专门的节拍数据表，存储音频文件与对应节拍时刻的关联关系。
4.  **对比功能**：提供专门的界面，将两种算法的自动识别结果与手动标记结果在同一波形图上进行可视化对比，并计算如F1分数、准确率、召回率等指标，用数据直观展示不同算法在该音频上的性能差异，帮助用户理解和选择。
5.  **功能进阶**：实现实时节拍预测与自学习系统的功能



## 系统核心功能模块

|      模块名称      | 核心功能                                                     |
| :----------------: | :----------------------------------------------------------- |
| **音乐波形可视化** | - 使用WaveSurfer.js显示音频波形图<br/>- 可以缩放和播放/暂停音频 |
| **节拍可视化展示** | \- 从数据库读取节拍时刻数据<br/>\- 在波形图上标记节拍点位置<br/>\- 提供节拍数据的增删改查接口<br/>\- 支持手动标记与自动识别结果的对比显示 |
|  **自动节拍识别**  | \- 集成传统信号处理节拍识别算法<br/>\- 提供深度学习节拍识别模型<br/>\- 支持算法参数调整和效果对比 |
|    **系统管理**    | 用户管理、角色权限管理、操作日志。                           |
| **数据可视化对比** | 支持不同方法得到的节拍数据在同一波形图上进行对比             |



## 数据库设计

### 节拍数据表 (BeatData)

|   字段名    |     类型     |           描述           |
| :---------: | :----------: | :----------------------: |
|     id      |  BIGINT(20)  |          主键ID          |
|  music_id   |     INT      |          音频ID          |
| beat_times  |     TEXT     |  节拍时刻数组(JSON格式)  |
| create_time |   DATETIME   |         创建时间         |
| update_time |   DATETIME   |         更新时间         |
| created_by  |  BIGINT(20)  |        创建用户ID        |
|  file_path  | VARCHAR(255) | 文件在服务器上的存放路径 |



### 相关用户信息表 (User)

|    **字段名**     |   **类型**   |        描述        |
| :---------------: | :----------: | :----------------: |
|        id         |     INT      | 用户ID(主键、自增) |
|       name        | VARCHAR(255) |       用户名       |
|     password      | VARCHAR(255) |        密码        |
|       email       | VARCHAR(255) |      电子邮箱      |
| registration_time |   DATETIME   |      注册时间      |
|    music_count    |     INT      |       音乐数       |
|  analysis_count   |     INT      |     分析记录数     |
|    view_count     |     INT      |      查看次数      |
|        bio        |     TEXT     |      个人介绍      |

说明：上表用于存储用户信息。



### 相关音乐信息表 (Music)

|     字段名     |     类型     |              说明              |
| :------------: | :----------: | :----------------------------: |
|       id       |     INT      |      音乐ID（主键、自增）      |
|      name      | VARCHAR(255) |              名称              |
|     author     | VARCHAR(255) |              作者              |
|  description   |     TEXT     |            内容介绍            |
| upload_user_id |     INT      | 上传用户ID（外键，关联用户表） |
|  upload_time   |   DATETIME   |            上传时间            |
|   file_path    | VARCHAR(255) |    文件在服务器上的存放路径    |

说明：上表用于存储音乐记录信息



```sql
-- 删除旧表（如果存在）
DROP TABLE IF EXISTS `music`;

-- 创建新音乐表
CREATE TABLE `music` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '音乐ID',
  `name` VARCHAR(255) NOT NULL COMMENT '名称',
  `author` VARCHAR(255) NOT NULL COMMENT '作者',
  `description` TEXT COMMENT '内容介绍',
  `upload_user_id` BIGINT NOT NULL COMMENT '上传用户ID',
  `upload_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `file_path` VARCHAR(255) NOT NULL COMMENT '存放路径',

  -- 确保文件路径唯一（外键约束必需）
  UNIQUE INDEX `idx_file_path_unique` (`file_path`),
  UNIQUE INDEX `idx_music_name_unique` (`name`),
  -- 用户外键约束
  FOREIGN KEY (`upload_user_id`) REFERENCES `sys_user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 添加索引优化查询
ALTER TABLE `music` ADD INDEX `idx_upload_user_id` (`upload_user_id`);




-- 删除旧 BeatData 表（如果存在）
DROP TABLE IF EXISTS `BeatData`;

-- 创建新的节拍数据表 (BeatData)
CREATE TABLE `BeatData` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `music_name` VARCHAR(255) NOT NULL COMMENT '音乐名称',
  `beat_times` TEXT NOT NULL COMMENT '节拍时刻数组(JSON格式)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NOT NULL COMMENT '创建用户ID',
  `file_path` VARCHAR(255) NOT NULL COMMENT '文件在服务器上的存放路径',

  UNIQUE INDEX `idx_music_name_unique`(`music_name`)
    
  -- 外键约束（关联music表的音乐名称）
  FOREIGN KEY (`music_name`) REFERENCES `music`(`name`) ON DELETE CASCADE ON UPDATE CASCADE,

  -- 外键约束（关联music表的文件路径）
  FOREIGN KEY (`file_path`) REFERENCES `music`(`file_path`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 添加索引优化查询性能
ALTER TABLE `BeatData` ADD INDEX `idx_music_name` (`music_name`);
ALTER TABLE `BeatData` ADD INDEX `idx_file_path` (`file_path`);
```



## 技术栈

项目采用**三层式异构微服务架构**：

1. **前端层**：Vue3 实现交互式音频可视化
2. **业务服务层**：Spring Boot 处理核心业务逻辑
3. **计算服务层**：Python FastAPI 提供专业信号处理能力

通过 RESTful API 实现层间解耦，使 Java 的企业级能力与 Python 的科学计算生态优势互补



## 项目进展

- [x] **对音频文件进行本地存储**
- [x] **实现对“节拍时刻”的自动识别和本地存储**
- [x] **展示“节拍时刻”在原始音频的波形图上**
- [x] **实现在波形图上对节拍时刻进行动态增删改**
- [ ] **基于能量包络的峰值检测 VS 基于CRNN的节拍跟踪 的算法比较,以自动识别某音频的“节拍时刻”**
- [ ] **实现不同方法得到的“节拍时刻”数据在同一波形图进行对比（用不同颜色进行分类？）**
- [ ] **系统管理**
    - [ ] 账号注册
    - [ ] 管理员权限
    - [ ] 菜单权限
- [ ] **实时节拍预测与自学习**



### 对音频文件进行本地存储



### 实现对“节拍时刻”的自动识别和本地存储

#### 核心目标：增加一个功能模块，该模块能够：

1. **自动检测** 给定音频文件（如 `song1.mp3`）的节拍时刻。
2. **将检测结果**（音频文件名 + 节拍时刻列表）**存储到本地数据库**。

#### 实现步骤：

##### 第一步：选择并集成节拍检测库

1. `librosa`(最常用，易上手，功能强大) 
2. `madmom`(专门为MIR设计，节拍检测算法先进，但可能稍复杂)。


##### 第二步：设计数据库表结构

##### 第三步：实现功能逻辑（伪代码/思路）

使用 **Java(springboot)+Python (Flask) + librosa + MySQL+vue.js** 作为技术栈：

1. **用户上传/选择音频文件：**

    - 通过网页表单上传文件到指定目录。
    - 或者让用户选择服务器上已有的音频文件。

2. **触发节拍检测与存储：**

    - 用户点击一个按钮（如“检测并存储节拍”）。
    - 后端接收到请求，获取目标音频文件的路径。

3. **调用节拍检测库 (`librosa`示例)：**

    ```python
    from flask import Flask, request, jsonify
    import librosa
    import numpy as np
    import os
    import re
    
    app = Flask(__name__)
    
    @app.route('/get_beatData', methods=['GET', 'POST'])
    def analyze_audio():
        # 支持GET和POST两种请求方式
        if request.method == 'GET':
            # 从查询参数获取路径
            audio_path = request.args.get('audio_path')
        else:
            # 从JSON体获取路径
            data = request.get_json(silent=True)
            audio_path = data.get('audio_path') if data else None
        
        if not audio_path:
            return jsonify({'error': 'No audio path provided'}), 400
        
        # 处理路径中的转义问题 - 使用字符串替换代替正则表达式
        audio_path = audio_path.replace('\\', '\\\\')  # 替换单反斜杠为双反斜杠
        audio_path = audio_path.replace('/', os.path.sep)  # 替换正斜杠为系统分隔符
        
        # 处理路径中的引号问题
        audio_path = audio_path.strip("'\"")
        
        if not os.path.exists(audio_path):
            return jsonify({'error': f'File not found: {audio_path}'}), 404
        
        try:
            # 加载音频文件
            y, sr = librosa.load(audio_path)
            
            # 计算节拍时间 - 使用更敏感的参数
            tempo, beat_frames = librosa.beat.beat_track(
                y=y, 
                sr=sr,
                hop_length=512,
                start_bpm=120,
                units='frames',
                tightness=100
            )
            beat_times = librosa.frames_to_time(beat_frames, sr=sr)
            
            # 如果检测到的节拍过少，尝试使用onset检测作为补充
            if len(beat_times) < 20:
                onset_frames = librosa.onset.onset_detect(
                    y=y, 
                    sr=sr,
                    hop_length=512,
                    backtrack=True,
                    units='frames'
                )
                onset_times = librosa.frames_to_time(onset_frames, sr=sr)
                
                # 合并beat_times和onset_times，去重并排序
                all_times = np.unique(np.concatenate([beat_times, onset_times]))
                beat_times = all_times
            
            # 将numpy数组转换为Python列表
            beat_times_list = beat_times.tolist()
            beat_times_list = [round(time, 2) for time in beat_times_list]
            
            return jsonify({
                'tempo': float(tempo),
                'beat_times': beat_times_list,
                'beat_count': len(beat_times_list),
                'audio_path': audio_path,
                'audio_duration': float(len(y) / sr)
            })
        
        except Exception as e:
            return jsonify({'error': str(e), 'audio_path': audio_path}), 500
    
    if __name__ == '__main__':
        app.run(host='localhost', port=5000, debug=True)
    ```

##### 第四步： 数据库交互

将 `audio_filename`和 `beat_times`列表（转换为 JSON 字符串或利用数据库的 JSON 类型）存入对应的字段。

##### 第五步：另起一个页面，管理分析后的数据信息

将`beat_detections`表的信息展现在前端，可进行增删改查操作，最重要的是提供跳转波形图上展示的接口。

##### 总结

1. **自动化检测：** 使用 `librosa`或 `madmom`等库**自动**计算节拍时刻，**无需手动记录**。
2. **数据库设计：** 创建包含 `audio_filename`(字符串) 和 `beat_times`(存储为 JSON 数组或可解析的字符串) 字段的表。
3. **后端逻辑：**
    - 接收音频文件或文件名。
    - 调用库进行节拍检测，得到时间列表。
    - 将文件名和时间列表存入数据库。



### 展示“节拍时刻”在原始音频的波形图上

#### 步骤1：从数据库读取节拍时刻数据

#### 步骤2：前端页面集成wavesurfer.js

#### 步骤3：初始化波形图并加载节拍标记

#### 步骤4：添加CSS样式（增强可视化效果）

#### 实现效果：

1. **音频波形可视化**：显示音频的完整波形
2. **红色节拍标记**：在波形图上以红色垂直线标记节拍点
3. **时间轴标记**：在顶部时间轴显示节拍位置
4. **交互功能**：
    - 播放/暂停控制
    - 点击跳转到任意位置
    - 缩放时间轴
    - 查看节拍点精确时间
5. **编辑功能**（备选）：
    - 添加新节拍点
    - 删除错误节拍点
    - 保存修改到数据库



### 实现在波形图上对节拍时刻进行动态增删改



### 基于能量包络的峰值检测 VS 基于CRNN的节拍跟踪 的算法比较,以自动识别某音频的“节拍时刻”

#### 方案1：传统方法 - **基于能量包络的峰值检测**

##### 原理

1. 计算音频短时能量（STFT → 频谱 → 求和）
2. 提取能量包络线（希尔伯特变换或平滑滤波）
3. 检测包络线上的局部峰值作为节拍候选点
4. 通过动态规划拟合节奏周期（消除异常点）

##### 优点：

1. 结构简单，易于实现
2. 运算量小，速度快
3. 可实时处理
4. 无需训练数据
5. 对打击乐/舞曲/鼓点音乐效果显著

##### 缺点：

1. 依赖能量突变 → 对鼓点型音乐偏好强
2. 人声/旋律/古典音乐中节拍不明显时容易失败
3. 对噪声敏感（峰值容易误检）
4. 难以处理弱节奏、慢节奏、多变节奏
5. 无法追踪全局节奏结构（只看局部峰值）
6. 易出现倍频/半频节拍误判
7. 无法利用音高、和声、非打击节奏特征

##### 代码实现

```python
暂无
```

------

#### 方案2：DP动态规划法

##### 原理：

- 先估计节拍周期（如通过自相关/频谱分析）
- 根据候选节拍点打分（如Onset强度/CQT特征）
- 用**动态规划**在时间序列中寻找“最优节拍路径”

即：局部峰值 → 全局最优节拍序列

##### 优点：

1. 适用于弱节奏、非鼓点音乐。如古典、爵士、民族音乐等 （只要有周期结构即可）
2. 易于结合音乐知识。如节拍间隔范围（40~220 BPM）、拍子层级、小节结构等
3. 算法可解释、调试方便。每个节拍点来源明确（Onset + 路径评分）
4. 不依赖海量训练数据。几乎不需要标注集，即可在多风格音乐上工作。
5. 可处理节拍不均匀情况。DP可以“跳过”或“补全”弱能源节拍点

##### 缺点：

1. 参数依赖强.拍长估计不准，会导致整体偏移。Onset检测好坏直接影响结果
1. 难应对复杂节奏/渐变曲风.如变速电子乐、即兴爵士、自由节拍演奏
1. 没有自动学习能力.只能依靠规则、人为设计的特征
1. 实时性一般

DP遍历会带来一定延迟

---

#### 方案3：CNN / CRNN 神经网络节拍检测

##### 原理

- 将节拍检测视为：
    →“语音/鼓点/节奏事件分类问题”
- 输入：CQT谱 / Mel谱 / Tempogram / Log-amplitude谱
- 输出：每一帧是否属于节拍点
- CNN 负责特征提取
- RNN/LSTM 捕捉时序节奏关系

##### 代表模型：

- Madmom CRNN

##### 优点：

1. 不依赖手工特征。CQT/Mel谱输入即可，自动学习节拍模式
1. 能适应多风格音乐。流行 / 古典 / 爵士 / EDM / Hip-hop / 民谣 等统一处理
1. 鲁棒性强。即使音量弱、人声强、乐器干扰，也能检测到节拍
1. 可处理变速节拍。如 rit./accel.、自由节奏、乱拍子
1. 精度高。在 MIREX 等测试中可达 85%~95%
1. 可实时/流式推理。延迟可控制在 10~50 ms

##### 缺点：

1. 需要大量标注数据。上万条音乐 + 精确节拍标注 = 成本高
2. 黑箱，不可解释。很难知道模型“凭什么认为那是节拍点”
3. 训练资源要求高。GPU训练 + 参数调优 + 数据增强
4. 对领域迁移敏感。训练集风格偏差会导致泛化问题（如只用西方音乐训练，可能不适合中国民乐）
5. 输出需后处理。如峰值抑制、动态规划追踪、平滑滤波等

##### 代码实现

```python
暂无
```



传统节拍检测方法多依赖音频能量包络及峰值定位（如频带能量差分、包络激活函数等），在鼓点型音乐中表现良好，但对弱节奏和非打击乐音乐适应性差。动态规划方法在此基础上引入拍长约束与全局搜索策略，通过优化节拍路径克服局部峰值缺失问题。近年来，基于CNN或CRNN的深度学习方法逐步成为主流，它们能够从时频谱中自动学习节拍结构，覆盖多风格音乐，但依赖海量标注数据且可解释性较弱。





### 实现不同方法得到的“节拍时刻”数据在同一波形图进行对比（用不同颜色进行分类？）



### 系统管理



### 实时节拍预测与自学习
