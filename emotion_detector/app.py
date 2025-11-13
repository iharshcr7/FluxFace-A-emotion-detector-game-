from flask import Flask, request, jsonify
import cv2
import numpy as np
from deepface import DeepFace  # 🔹 Import the main DeepFace library

app = Flask(__name__)

# 🔹 Health check route
@app.route("/", methods=["GET"])
def home():
    return "🎯 Emotion API is running!", 200

@app.route("/predict", methods=["POST"])
def predict():
    try:
        file = request.files["image"]
        img_bytes = file.read()

        # Convert to OpenCV image
        nparr = np.frombuffer(img_bytes, np.uint8)
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        # 🔹 --- NEW ACCURACY LOGIC --- 🔹
        
        # 1. Analyze for all emotion scores, not just the top one.
        #    enforce_detection=False prevents a crash if no face is found.
        result = DeepFace.analyze(frame, actions=['emotion'], enforce_detection=False)

        # 2. 'result' is a list (for multiple faces), so we take the first one.
        if isinstance(result, list) and len(result) > 0:
            emotions = result[0]['emotion']          # This is a dict of all scores
            dominant_emotion = result[0]['dominant_emotion'] # This is the top-scoring emotion

            # 🔹 --- THIS IS THE NEW CLEAN PRINT --- 🔹
            print("\n======================================")
            print("   EMOTION DETECTION REPORT")
            print("======================================")
            # Loop through and print each emotion neatly formatted
            for emotion, score in emotions.items():
                print(f"   - {emotion.capitalize():<10}: {score:6.2f} %")
            print("--------------------------------------")
            print(f"   Dominant Emotion: {dominant_emotion.capitalize()}")
            # 🔹 ----------------------------------- 🔹

            # 3. 🔹 --- NEW, SMARTER RULE --- 🔹
            #    We only override to 'happy' if the person is mostly 'neutral'
            #    or 'surprised', but is also smiling.
            if emotions['happy'] > 25.0 and (dominant_emotion == 'neutral' or dominant_emotion == 'surprise'):
                final_emotion = 'happy'
            else:
                # Otherwise, we trust the dominant (strongest) emotion.
                final_emotion = dominant_emotion 
            
            # This line below is also new, to show the final decision in the terminal
            print(f"   Final Decision:   {final_emotion.capitalize()}")
            print("======================================\n")
            
            return jsonify({"emotion": final_emotion})
        
        else:
            # If no face was detected at all, default to neutral
            print("\n======================================")
            print("   --- NO FACE DETECTED ---") 
            print("======================================\n")
            return jsonify({"emotion": "neutral"})

    except Exception as e:
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)