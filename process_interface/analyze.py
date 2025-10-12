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
        if isinstance(tempo, (list, tuple, np.ndarray)):
            tempo_value = float(tempo[0])
        else:
            tempo_value = float(tempo)

        return jsonify({
            'tempo': tempo_value,
            'beat_times': beat_times_list,
            'beat_count': len(beat_times_list),
            'audio_path': audio_path,
            'audio_duration': float(len(y) / sr)
        })
    
    except Exception as e:
        return jsonify({'error': str(e), 'audio_path': audio_path}), 500

if __name__ == '__main__':
    app.run(host='localhost', port=5000, debug=True)