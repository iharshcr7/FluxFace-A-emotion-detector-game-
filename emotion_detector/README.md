# Emotion Detection API - Accuracy Improvements

## Changes Made for Better Accuracy

### 1. Simplified to 3 Emotions
The system now maps all detected emotions to just 3 categories:
- **Happy** - Positive emotions
- **Neutral** - Neutral/ambiguous emotions
- **Sad** - Negative emotions (angry, fear, disgust, sad)

### 2. Emotion Mapping
```python
'happy' → 'happy'
'neutral' → 'neutral'
'sad' → 'sad'
'angry' → 'sad'
'fear' → 'sad'
'disgust' → 'sad'
'surprise' → 'neutral'
```

### 3. Confidence Threshold
- Added a 40% confidence threshold
- If emotion confidence is below 40%, defaults to "neutral"
- This reduces false positives

### 4. Better Image Quality
- Switched from grayscale to color images
- DeepFace models work better with color information
- Facial features are more distinct in color

### 5. Logging
- Added detailed logging to track detection results
- Shows raw emotion, confidence scores, and final result
- Helps debug and tune the system

## Installation

```bash
cd emotion_detector
pip install -r requirements.txt
```

## Running the Server

```bash
python app.py
```

The server will start on `http://0.0.0.0:5000`

## Tuning for Better Accuracy

### Adjust Confidence Threshold
In `app.py`, line 58, change the threshold:
```python
if dominant_score < 40:  # Change this value
```
- **Lower** (e.g., 30) = More sensitive, may have false positives
- **Higher** (e.g., 50) = More conservative, more neutrals

### Change Detection Backend
In `app.py`, line 35, change the detector:
```python
detector_backend='opencv'  # Options: opencv, retinaface, mtcnn, ssd
```
- **opencv** - Fast, good for mobile (current)
- **retinaface** - Most accurate, slower
- **mtcnn** - Good balance
- **ssd** - Fast, less accurate

### Adjust Emotion Mapping
If you want "surprise" to map to "happy" instead of "neutral", modify the mapping in `app.py`:
```python
'surprise': 'happy'  # Instead of 'neutral'
```

## Testing Tips

1. **Good Lighting** - Ensure face is well-lit
2. **Face Position** - Face should be centered and clearly visible
3. **Expression** - Make clear, exaggerated expressions for testing
4. **Distance** - Keep appropriate distance from camera
5. **Single Face** - Works best with one face in frame

## Android App Configuration

Update the server URL in `MainActivity4.java` (line 66):
```java
private static final String SERVER_URL = "http://YOUR_IP:5000/predict";
```

Replace `YOUR_IP` with your computer's local IP address.

## Difficulty Mapping
- **Happy** → Hard difficulty
- **Neutral** → Normal difficulty  
- **Sad** → Easy difficulty

## Troubleshooting

### Low Accuracy?
1. Check the server logs to see confidence scores
2. Ensure good lighting conditions
3. Try different detector backends
4. Adjust confidence threshold
5. Use color images (already enabled)

### Server Connection Issues?
1. Ensure both devices are on the same network
2. Check firewall settings
3. Verify the IP address is correct
4. Test with `curl http://YOUR_IP:5000/` to check if server is reachable

### "No face detected" errors?
1. Improve lighting
2. Center your face in frame
3. Move closer to camera
4. Try `enforce_detection=False` in `app.py` line 34 for more lenient detection
