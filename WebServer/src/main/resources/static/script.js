const startButton = document.getElementById("startButton");
const stopButton = document.getElementById("stopButton");
const statusText = document.getElementById("status");
const transcriptionText = document.getElementById("transcription");

let mediaRecorder;
let audioChunks = [];
let mediaStream;

startButton.addEventListener("click", async () => {
    try {
        mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });

        audioChunks = [];

        mediaRecorder = new MediaRecorder(mediaStream);

        mediaRecorder.addEventListener("dataavailable", event => {
            if (event.data.size > 0) {
                audioChunks.push(event.data);
            }
        });

        mediaRecorder.addEventListener("stop", async () => {
            const audioBlob = new Blob(audioChunks, {
                type: "audio/webm"
            });

            const formData = new FormData();
            formData.append("file", audioBlob, "recording.webm");

            statusText.textContent = "Transcribing...";

            try {
                const response = await fetch("/api/v1/transcriptions", {
                    method: "POST",
                    body: formData
                });

				if (!response.ok) {
				    const errorResponse = await response.json();

				    throw new Error(
				        errorResponse.message || ("Server returned " + response.status)
				    );
				}

                const result = await response.json();

                transcriptionText.textContent = result.text;
                statusText.textContent = "Transcription complete.";

				} catch (error) {
				    statusText.textContent =
				        "Transcription failed: " + error.message;
				}
        });

        mediaRecorder.start();

        startButton.disabled = true;
        stopButton.disabled = false;

        statusText.textContent = "Recording...";

    } catch (error) {
        statusText.textContent =
            "ERROR: " + error.name + " - " + error.message;
    }
});

stopButton.addEventListener("click", () => {
    if (mediaRecorder && mediaRecorder.state !== "inactive") {
        mediaRecorder.stop();

        mediaStream.getTracks().forEach(track => track.stop());

        startButton.disabled = false;
        stopButton.disabled = true;
    }
});