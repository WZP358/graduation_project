<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="12" style="padding-left: 20px">
        <h2>音频节拍自动识别平台</h2>
        <p>
          &emsp;&emsp;音乐节拍是音乐结构中最基础的组成部分，它构成了音乐的骨架和律动感。音频节拍自动识别作为音乐信息检索领域的核心课题之一，旨在让计算机能够像人类乐手或听众一样，实时地、准确地感知并定位音频信号中的节拍点。这项技术对于音乐分析、自动伴奏系统、音乐教育、舞蹈游戏、健身软件（如跑步节奏匹配）以及DJ辅助打碟等众多领域都具有极其重要的应用价值。
        </p>
        <p>
          &emsp;&emsp;传统的节拍识别方法主要基于信号处理技术，例如通过计算短时能量、频谱通量、自相关函数等特征来检测信号的周期性脉冲。这些方法在节奏规整的流行音乐或电子音乐上表现尚可，但其局限性也十分明显：它们严重依赖于预设的阈值和启发式规则，对于节奏复杂、速度多变（如 rubato）、或打击乐不明显的音乐类型（如古典音乐、爵士乐），其识别准确率会显著下降，缺乏足够的鲁棒性。
        </p>
        <p>
          &emsp;&emsp;近年来，随着深度学习技术的飞速发展，尤其是卷积神经网络（CNN）和循环神经网络（RNN）在时序信号处理上的成功应用，基于数据驱动的节拍识别方法取得了突破性进展。这些模型能够从海量音频数据中自动学习高层次、抽象的特征表示，从而更有效地建模音乐中的时序依赖关系，在处理复杂音乐场景时展现出远超传统方法的潜力。
        </p>
        <p>
          &emsp;&emsp;尽管底层算法研究已硕果累累，但将其转化为用户可用的实用工具仍然存在缺口。当前主流的大型音乐可视化与编辑平台（如 Audacity、Sonic Visualiser 等）虽然功能强大，但其设计重心在于通用音频编辑和基础分析，操作逻辑相对复杂，且普遍缺乏集成的、端到端的节拍自动识别功能。用户往往需要手动标注节拍点，这个过程不仅枯燥耗时，而且结果带有极强的主观性，难以保证一致性和准确性。
        </p>
        <p>
          &emsp;&emsp;因此，本项目旨在开发一个专门化的音频节拍自动识别平台。该平台将致力于融合传统信号处理方法的可解释性与深度学习模型的高精度优势，提供一个集音频数据可视化、自动节拍识别、节拍数据管理及结果对比验证于一体的完整解决方案。最终目标是降低这项技术的使用门槛，为音乐研究者、开发者和爱好者提供一个高效、可靠且用户友好的工具，以推动节拍识别技术从学术研究走向实际应用。
        </p>

      </el-col>

      <el-col :sm="24" :lg="12" style="padding-left: 50px">
        <el-row>
          <el-col :span="12">
            <h2>技术选型</h2>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="6">
            <h4>后端技术</h4>
            <ul>
              <li>SpringBoot</li>
              <li>Spring Security</li>
              <li>JWT</li>
              <li>MyBatis</li>
              <li>Druid</li>
              <li>Fastjson</li>
              <li>...</li>
            </ul>
          </el-col>
          <el-col :span="6">
            <h4>前端技术</h4>
            <ul>
              <li>Vue</li>
              <li>Vuex</li>
              <li>Element-ui</li>
              <li>Axios</li>
              <li>Sass</li>
              <li>Quill</li>
              <li>...</li>
            </ul>
          </el-col>
          <el-col :span="6">
            <h4>算法</h4>
            <ul>
              <li>Python</li>
              <li>FastAPI</li>
              <li>...</li>
            </ul>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
    <el-divider />
    <el-row :gutter="20">

    </el-row>
  </div>
</template>

<script>
export default {
  name: "Index",
  data() {
    return {
      // 版本号
      version: "3.9.0"
    }
  },
  methods: {
    goTarget(href) {
      window.open(href, "_blank")
    }
  }
}
</script>

<style scoped lang="scss">
.home {
  blockquote {
    padding: 10px 20px;
    margin: 0 0 20px;
    font-size: 17.5px;
    border-left: 5px solid #eee;
  }
  hr {
    margin-top: 20px;
    margin-bottom: 20px;
    border: 0;
    border-top: 1px solid #eee;
  }
  .col-item {
    margin-bottom: 20px;
  }

  ul {
    padding: 0;
    margin: 0;
  }

  font-family: "open sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 13px;
  color: #676a6c;
  overflow-x: hidden;

  ul {
    list-style-type: none;
  }

  h4 {
    margin-top: 0px;
  }

  h2 {
    margin-top: 10px;
    font-size: 26px;
    font-weight: 100;
  }

  p {
    margin-top: 10px;

    b {
      font-weight: 700;
    }
  }

  .update-log {
    ol {
      display: block;
      list-style-type: decimal;
      margin-block-start: 1em;
      margin-block-end: 1em;
      margin-inline-start: 0;
      margin-inline-end: 0;
      padding-inline-start: 40px;
    }
  }
}
</style>

