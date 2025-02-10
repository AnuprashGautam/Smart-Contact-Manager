package com.smart.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smart.dao.UserRepository;
import com.smart.entities.User;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@RequestMapping("/index")
	public String dashboard(Model model,Principal principal)
	{
		String userName=principal.getName();
		
		// Get the user using the username i.e. email.
		User user=userRepository.getUserByUserName(userName);
		
		model.addAttribute("user",user);
		
		return "normal/user_dashboard";
	}
}
