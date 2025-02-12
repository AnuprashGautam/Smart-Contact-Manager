package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;
import com.smart.services.SessionHelper;

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
	public String processContact(@ModelAttribute Contact contact,
								Principal principal,
								@RequestParam("profileImage") MultipartFile file,
								HttpSession session) 
	{
		boolean b=false;  // For storing the status.
		try {
			String name=principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			
			// Processing and uploading file(Image).
			if(!file.isEmpty())
			{
				contact.setImage(file.getOriginalFilename());
				
				File saveFile = new ClassPathResource("/static/img").getFile(); // The path where profileImage is going to be saved.
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(),path , StandardCopyOption.REPLACE_EXISTING);
				
				b=true;
				System.out.println("Image uploaded successfully.");
			}
			
			
			contact.setUser(user);					// Setting user in the contact.
			user.getContacts().add(contact);		// Adding contact in the user's contact attribute.
			
			
			
			this.userRepository.save(user);
			
			// For add contact form.
			if(b) {
				session.setAttribute("message",new Message("Contact added Successfully!!!","alert-success"));
			}else {
				session.setAttribute("message",new Message("Something went wrong!!! Please add the profile photo.","alert-danger"));
			}
			
			// For the back-end console.
			System.out.println("Contact added successfully.");
		} catch (Exception e) {
			e.printStackTrace();
			session.setAttribute("message",new Message("Something went wrong!!!","alert-danger"));
		}
		
		return "normal/add_contact_form";
	}
}
