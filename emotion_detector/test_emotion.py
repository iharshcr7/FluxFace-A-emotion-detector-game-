"""
Quick test script to verify emotion detection is working
"""
import cv2
from deepface import DeepFace

def test_emotion():
    print("Testing emotion detection...")
    print("=" * 50)
    
    # Try to capture from webcam
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("ERROR: Cannot open camera")
        return
    
    print("Camera opened successfully!")
    print("Press 'c' to capture and analyze, 'q' to quit")
    
    while True:
        ret, frame = cap.read()
        if not ret:
            print("Failed to grab frame")
            break
        
        # Show the frame
        cv2.imshow('Test Emotion Detection - Press C to analyze', frame)
        
        key = cv2.waitKey(1) & 0xFF
        
        if key == ord('c'):
            print("\nAnalyzing emotion...")
            try:
                analysis = DeepFace.analyze(
                    frame, 
                    actions=['emotion'], 
                    enforce_detection=False,
                    detector_backend='opencv',
                    silent=True
                )
                
                if isinstance(analysis, list) and len(analysis) > 0:
                    emotions = analysis[0]['emotion']
                    dominant = analysis[0]['dominant_emotion']
                    dominant_score = emotions[dominant]
                    
                    print(f"\nRaw Detection:")
                    print(f"  Dominant Emotion: {dominant}")
                    print(f"  Confidence: {dominant_score:.2f}%")
                    print(f"\nAll Scores:")
                    for emotion, score in emotions.items():
                        print(f"  {emotion}: {score:.2f}%")
                    
                    # Apply mapping
                    emotion_map = {
                        'happy': 'happy',
                        'neutral': 'neutral',
                        'sad': 'sad',
                        'angry': 'sad',
                        'fear': 'sad',
                        'disgust': 'sad',
                        'surprise': 'neutral'
                    }
                    
                    if dominant_score < 25:
                        final = "neutral (low confidence)"
                    else:
                        final = emotion_map.get(dominant.lower(), 'neutral')
                    
                    print(f"\nFinal Result: {final}")
                    print(f"Game Difficulty: {'HARD' if final.startswith('happy') else 'EASY' if final.startswith('sad') else 'NORMAL'}")
                    print("=" * 50)
                else:
                    print("No face detected!")
                    
            except Exception as e:
                print(f"Error: {e}")
        
        elif key == ord('q'):
            break
    
    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    test_emotion()
