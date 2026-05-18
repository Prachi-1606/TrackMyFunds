/*
 * TrackMyFunds — voice input for the natural-language expense quick-entry.
 * Uses the browser-native Web Speech API (Chrome/Edge); silently disables
 * itself on unsupported browsers.
 */
(function () {
    'use strict';

    const micBtn          = document.getElementById('nlMicBtn');
    const nlInput         = document.getElementById('nlInput');
    const parseBtn        = document.getElementById('nlParseBtn');
    const listeningText   = document.getElementById('nlListeningText');
    const unsupportedHint = document.getElementById('nlVoiceUnsupported');

    if (!micBtn || !nlInput || !parseBtn) return; // Not on a page with NL entry

    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;

    if (!SR) {
        // Browser doesn't support the Web Speech API — hide the button and
        // surface the small "requires Chrome or Edge" hint.
        micBtn.classList.add('d-none');
        if (unsupportedHint) unsupportedHint.classList.remove('d-none');
        return;
    }

    const recognition = new SR();
    recognition.lang            = 'en-IN';
    recognition.continuous      = false;
    recognition.interimResults  = true;

    let isRecording    = false;
    let finalTranscript = '';

    function setRecording(recording) {
        isRecording = recording;
        micBtn.classList.toggle('tmf-mic-recording', recording);

        const icon = micBtn.querySelector('i');
        if (icon) {
            icon.className = recording ? 'bi bi-record-circle-fill' : 'bi bi-mic-fill';
        }
        micBtn.setAttribute('aria-pressed', String(recording));
        micBtn.title = recording ? 'Stop recording' : 'Speak your expense';

        if (listeningText) listeningText.classList.toggle('d-none', !recording);
    }

    micBtn.addEventListener('click', () => {
        if (isRecording) {
            recognition.stop();
            return;
        }
        finalTranscript = '';
        try {
            recognition.start();
        } catch (e) {
            // Most often: recognition.start() called while already started.
            setRecording(false);
        }
    });

    recognition.addEventListener('start', () => setRecording(true));

    recognition.addEventListener('result', (event) => {
        let interim = '';
        for (let i = event.resultIndex; i < event.results.length; i++) {
            const transcript = event.results[i][0].transcript;
            if (event.results[i].isFinal) {
                finalTranscript += transcript;
            } else {
                interim += transcript;
            }
        }
        // Show the running transcript live so the user sees feedback as they speak.
        nlInput.value = (finalTranscript + interim).trim();
    });

    recognition.addEventListener('error', () => {
        setRecording(false);
    });

    recognition.addEventListener('end', () => {
        setRecording(false);
        const text = finalTranscript.trim();
        if (text) {
            nlInput.value = text;
            // Hand off to the existing "Parse with AI" handler.
            parseBtn.click();
        }
    });
}());
