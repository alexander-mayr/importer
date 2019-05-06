package com.importservice.importservice;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class MessageController {
	@PostMapping("/messages/{uuid}/{step}/")
	public ResponseEntity<String> newReservation(@RequestBody String body, @PathVariable String uuid, @PathVariable Integer step) {
		Message message = new Message(uuid, body, step);
		HttpStatus status;

		if(message.queueUp()) {
			status = HttpStatus.ACCEPTED;
		}
		else {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		return new ResponseEntity<>("", status);
	}
}
