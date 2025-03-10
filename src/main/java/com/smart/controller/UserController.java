package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	
	// Adding common data to response.
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();

		// Get the user using the user-name i.e. email.
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
								HttpSession session,
								Model model) 
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
			
			// Sending the api key.
			model.addAttribute("api_key",apiKey);
			
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
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,Model model,HttpSession session,Principal principle)
	{
		
		try {
			
			//fetch old contact details
			Contact oldcontactDetails=this.contactRepository.findById(contact.getcId()).get();
			
			
			// Checking if the user want to update the photo or not.
			if(!file.isEmpty())
			{
				//delete file
				File deleteFile=new ClassPathResource("static/img").getFile();  // For path to that folder.
				File file1=new File(deleteFile,oldcontactDetails.getImage());
				file1.delete();
				
				//update image
				File saveFile=new ClassPathResource("static/img").getFile();
				
				Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
			}
			else
			{
				contact.setImage(oldcontactDetails.getImage());
			}
			
			User user=this.userRepository.getUserByUserName(principle.getName());
			
			contact.setUser(user);
			
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Contact updated successfully.","alert-success"));
			
		}catch(Exception e)
		{
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went wrong.","alert-danger"));
			
		}
		
		
		System.out.println("Conatct NAME="+contact.getName());
		System.out.println("Contact ID="+contact.getcId());
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	
	// Handler method to show the profile details.
	@GetMapping("/profile")
	public String profileHandler(Model model, Principal principal) {
		
		String userName = principal.getName();
		
		User user = this.userRepository.getUserByUserName(userName);
				
		model.addAttribute("title",user.getName());
		model.addAttribute("user",user);
		
		return "normal/profile";
	}
	
	//Handler method to update the profile.
	@PostMapping("/update-profile")
	public String updateUserProfile(Model model, Principal principal) {
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		//Sending the api key to the template so that the respective service can be used.
		model.addAttribute("api_key",apiKey);
		model.addAttribute("user",user);
		model.addAttribute("title",user.getName()+"- Update Profile");
		
		return "normal/update_profile_form";
	}
	
	@PostMapping("/process-update-profile")
	public String processUpdateProfile(Model model,
			@ModelAttribute User user,
			@RequestParam("newImage") MultipartFile file, 
			HttpSession session,
			Principal principal) 
	{
		try {
			User oldUserDetail=this.userRepository.getUserByUserName(principal.getName());
			
			// Checking if the user has provided the new profile photo ,or not.
			if(!file.isEmpty()) {
				//delete file
				File deleteFile=new ClassPathResource("static/img").getFile();  // For path to that folder.
				File file1=new File(deleteFile,oldUserDetail.getImageUrl());
				file1.delete();
				
				//update image
				user.setImageUrl(file.getOriginalFilename());
				
				File saveFile=new ClassPathResource("static/img").getFile();
				
				Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			else {
				user.setImageUrl(oldUserDetail.getImageUrl());
			}
			
			this.userRepository.save(user);
			
			session.setAttribute("message",new Message("Profile updated successfully.","alert-success"));
		} catch (Exception e) {
			e.printStackTrace();
			session.setAttribute("message",new Message("Something went wrong!!!","alert-danger"));
		}
		
		return "normal/update_profile_form";
	}
	
	
	// open setting handler
	@GetMapping("/settings")
	public String openSettings(Model model) {
		
		model.addAttribute("title","Settings");
		
		return "normal/settings";
	}
	
	// Change password handler
	@PostMapping("/change-password")
	public String changePassword(Principal principal,
								@RequestParam("oldPassword") String oldPassword,
								@RequestParam("newPassword") String newPassword,
								HttpSession session) 
	{
		System.out.println("OLD_PASSWORD: "+oldPassword);		
		System.out.println("NEW_PASSWORD: "+newPassword);	
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		System.out.println(user.getPassword());
		
		if(this.bCryptPasswordEncoder.matches(oldPassword, user.getPassword())) 
		{
			// Changing the password and returning the success message.
			user.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			
			this.userRepository.save(user);
			
			session.setAttribute("message", new Message("Password updated successfully.", "alert-success"));
		}else 
		{
			// Just returning the failure message.

			session.setAttribute("message", new Message("Password didn't matched with the old password. Something went wrong!!!", "alert-danger"));
		}
		
		return "redirect: user/settings";
	}
}








