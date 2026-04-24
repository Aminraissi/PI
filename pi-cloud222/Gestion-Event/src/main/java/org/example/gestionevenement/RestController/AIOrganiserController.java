package org.example.gestionevenement.RestController;

import lombok.AllArgsConstructor;
import org.example.gestionevenement.DTO.ChatOrganiserRequest;
import org.example.gestionevenement.DTO.ChatOrganiserResponse;
import org.example.gestionevenement.Services.AIService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/ai_event")
public class AIOrganiserController {

    private AIService aiService;

    @PostMapping("/chat")
    public ChatOrganiserResponse chat(@RequestBody ChatOrganiserRequest request) {

        String userText = request.getMessages()
                .get(request.getMessages().size() - 1)
                .getContent();

        String prompt = """
                You are an agricultural event assistant in Tunisia.
                Recommend location, date, weather, and participants.
                
                User: """ + userText;

        String reply = aiService.askAI(prompt);

        return new ChatOrganiserResponse(reply);
    }

}
