package com.smart.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;

	
	// Adding common data to response.
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();

		// Get the user using the username i.e. email.
		User user = userRepository.getUserByUserName(userName);

		model.addAttribute("user", user);
	}

	// For handling the user dashboard.
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title","User dashboard");
		return "normal/user_dashboard";
	}

	// Open add form handler.
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact",new Contact());
		
		return "normal/add_contact_form";
	}
	
	// Processing add contact form.
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, Principal principal, HttpSession session) {
				
		String name=principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		
		contact.setUser(user);					// Setting user in the contact.
		user.getContacts().add(contact);		// Adding contact in the user's contact attribute.
		
		this.userRepository.save(user);
		
		// For add contact form.
		session.setAttribute("message",new Message("Contact added Successfully!!!","alert-success"));
		
		// For the backend console.
		System.out.println("Contact added successfully.");
		
		return "normal/add_contact_form";
	}
}
