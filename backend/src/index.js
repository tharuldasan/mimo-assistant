const jsonHeaders = {
  "Content-Type": "application/json; charset=UTF-8",
  "Cache-Control": "no-store"
};

const mimoInstruction = `You are Mimo, a warm, playful, concise AI companion inside an Android app.
Reply in the user's language: Sinhala if they write Sinhala, otherwise English.
Never claim to be secretly listening, watching the screen, sending messages, or controlling the phone.
Ask for confirmation before suggesting a consequential phone action. Keep most replies under 80 words.`;

export default {
  async fetch(request, env) {
    if (request.method !== "POST") {
      return new Response(JSON.stringify({ error: "Use POST." }), { status: 405, headers: jsonHeaders });
    }

    try {
      const body = await request.json();
      const message = typeof body.message === "string" ? body.message.trim() : "";
      const language = typeof body.language === "string" ? body.language : "en-US";

      if (!message || message.length > 1500) {
        return new Response(JSON.stringify({ error: "Message must contain 1 to 1500 characters." }), {
          status: 400,
          headers: jsonHeaders
        });
      }

      const geminiResponse = await fetch(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "x-goog-api-key": env.GEMINI_API_KEY
          },
          body: JSON.stringify({
            systemInstruction: {
              parts: [{ text: mimoInstruction }]
            },
            contents: [{
              role: "user",
              parts: [{ text: `Language preference: ${language}\nUser: ${message}` }]
            }],
            generationConfig: {
              temperature: 0.85,
              maxOutputTokens: 220
            }
          })
        }
      );

      const data = await geminiResponse.json();
      const reply = data.candidates?.[0]?.content?.parts?.map((part) => part.text ?? "").join("").trim();

      if (!geminiResponse.ok || !reply) {
        console.log("Gemini request failed", geminiResponse.status);
        return new Response(JSON.stringify({ error: `Mimo's cloud brain is unavailable (Gemini HTTP ${geminiResponse.status}).` }), {
          status: 502,
          headers: jsonHeaders
        });
      }

      return new Response(JSON.stringify({ reply }), { headers: jsonHeaders });
    } catch (error) {
      console.log("Request failed", error instanceof Error ? error.message : "unknown error");
      return new Response(JSON.stringify({ error: "Mimo could not process that request." }), {
        status: 500,
        headers: jsonHeaders
      });
    }
  }
};
