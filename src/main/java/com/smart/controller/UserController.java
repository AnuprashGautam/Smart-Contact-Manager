package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Value("${tinymce.api.key}") // Fetching the api key from the .env file.
    private String apiKey;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;

	
	// Adding common data to response.
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();

		// Get the user using the username i.e. email.
		User user = userRepository.getUserByUserName(userName);

		model.addAttribute("user", user);
	}

	// For handling the user dash-board.
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
		
		//Sending the api key to the template so that the respective service can be used.
		model.addAttribute("api_key",apiKey);
		
		return "normal/add_contact_form";
	}
	
	// Processing add contact form.
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact,
								Principal principal,
								@RequestParam("profileImage") MultipartFile file,
								HttpSession session) 
	{
		try {
			String name=principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			
			// Processing and uploading file(Image).
			if(file.isEmpty()) {
				System.out.println("File is empty");
				contact.setImage("contact.png");
			}
			else
			{
				contact.setImage(file.getOriginalFilename());
				
				File saveFile = new ClassPathResource("/static/img").getFile(); // The path where profileImage is going to be saved.
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(),path , StandardCopyOption.REPLACE_EXISTING);
				
				System.out.println("Image uploaded successfully.");
			}
			
			
			contact.setUser(user);					// Setting user in the contact.
			user.getContacts().add(contact);		// Adding contact in the user's contact attribute.
			
			
			
			this.userRepository.save(user);
			
			// For add contact form.
			session.setAttribute("message",new Message("Contact added Successfully!!!","alert-success"));
			
			// For the back-end console.
			System.out.println("Contact added successfully.");
		} catch (Exception e) {
			e.printStackTrace();
			session.setAttribute("message",new Message("Something went wrong!!!","alert-danger"));
		}
		
		return "normal/add_contact_form";
	}
	
	
	// Show contacts handler
	// Contacts per page represented by "n".
	// Current page = 0 represented by "page".
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model model, Principal principal) {
		
		model.addAttribute("title","Show User Contacts");
		
		String userName= principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		// Fetching all the contacts of the logged in user.
		// This pageable object has two informations: current page - page,  contact per page - 8.
		Pageable pageable = PageRequest.of(page, 8);
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(),pageable);
		
		model.addAttribute("contacts",contacts);
		model.addAttribute("currentPage",page);
		model.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	// Showing particular contact details.
	@GetMapping("/{cId}/contact")
	public String  showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		
		System.out.println("Showing the details of the contact with id:"+cId);
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact= contactOptional.get();
				
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId()) {  // Only the contacts which the logged in user have, can be view his/her own contacts.
			model.addAttribute("contact",contact);
			model.addAttribute("title",  contact.getName()+"- Contact Details");
		}
		
		return "normal/contact_detail";
	}
	
	
	// Delete contact handler.
	@GetMapping("/delete/{cId}")
	public String deleteContact (@PathVariable("cId") Integer cId, Model model, Principal principal, HttpSession session) {
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact=contactOptional.get();
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId()) { // Only the contacts which the logged in user have, can be deleted by him/her.
			contact.setUser(null);						// To maintain the referential constraint.
			this.contactRepository.delete(contact);
			
			session.setAttribute("message",new Message("Contact deleleted successfully!!!", "alert-success"));
		}
		
		return "redirect:/user/show-contacts/0";	// redirect is basically used here to redirect the control on a path, not to any html page.
	}
	
	
	// Open update form handler
	@PostMapping("/update-contact/{cId}")
	public String updateForm(@PathVariable("cId") Integer cId ,Model model, Principal principal) {
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact= contactOptional.get();
		
		//Sending the api key to the template so that the respective service can be used.
		model.addAttribute("api_key",apiKey);
		model.addAttribute("contact",contact);
		model.addAttribute("title","Update Contact");
		
		return "normal/update_form";
	}
	
	
	// Handling the update contact form data and updating the respective contact.
	@PostMapping("/process-update")
	public String updateContact(Model model,HttpSession session) {
		
		session.setAttribute("message",new Message("Contact updated successfully!!!", "alert-success"));
		
		return "normal/update_form";
	}
}






