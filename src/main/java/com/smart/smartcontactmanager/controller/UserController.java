package com.smart.smartcontactmanager.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AbstractClassTestingTypeFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.smartcontactmanager.Repo.ContactRepository;
import com.smart.smartcontactmanager.Repo.UserRepository;
import com.smart.smartcontactmanager.entities.Contact;
import com.smart.smartcontactmanager.entities.User;
import com.smart.smartcontactmanager.helper.Message;

import javassist.expr.NewArray;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@ModelAttribute
	public void addCommonData(Model m, Principal principal) {
		String username = principal.getName();
		User user = this.userRepository.getUserByUserName(username);

		m.addAttribute("user", user);

	}

	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "This is Private dashboard");
		return "normal/user_dashboard";
	}

	// open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model,Principal principal) {
		model.addAttribute("title", "Add Contact");
	    model.addAttribute("contact", new Contact());

	    // Fetch user and contacts
	    String username = principal.getName();
	    User user = this.userRepository.getUserByUserName(username);
	    Pageable pageable = PageRequest.of(0, 4); // Adjust the page size as needed
	    Page<Contact> contacts = contactRepository.findContactsByUser(user.getId(), pageable);

	    // Add contacts to the model
	    model.addAttribute("contacts", contacts.getContent());
	    model.addAttribute("currentPage", 0);
	    model.addAttribute("totalPages", contacts.getTotalPages());

	    return "normal/add_contact_form";
	}

	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, Principal principal,
	                             @RequestParam("profileImage") MultipartFile file, HttpSession session, Model model) {
	    try {
	        String name = principal.getName();
	        User user = this.userRepository.getUserByUserName(name);
	        if (file.isEmpty()) {
	            System.out.println("file is empty");
	            contact.setImage("contact.png");
	        } else {
	            contact.setImage(file.getOriginalFilename());
	            File file2 = new ClassPathResource("static/img").getFile();
	            Path path = Paths.get(file2.getAbsolutePath() + File.separator + file.getOriginalFilename());
	            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
	            System.out.println("Image is uploaded");
	        }
	        contact.setUser(user);
	        user.getContacts().add(contact);
	        this.userRepository.save(user);
	        System.out.println("Data=" + contact);

	        System.out.println("Added to Database");
	        session.setAttribute("message", new Message("Your contact is added !! Add more", "success"));

	        // Add list of contacts to the model
	        Pageable pageable = PageRequest.of(0, 4);  // Assuming page 0 and size 4 for initial display
	        Page<Contact> contacts = contactRepository.findContactsByUser(user.getId(), pageable);
	        model.addAttribute("contacts", contacts);
	        model.addAttribute("currentPage", 0);
	        model.addAttribute("totalPages", contacts.getTotalPages());

	    } catch (Exception e) {
	        System.out.println("error" + e.getMessage());
	        e.printStackTrace();
	        session.setAttribute("message", new Message("Something went wrong!! Try again", "danger"));
	    }
	    return "normal/add_contact_form";
	}


	@GetMapping("/add_contact_form/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m, Principal principal) {
		m.addAttribute("title", "Show user contact");
		String name = principal.getName();
		User user = userRepository.getUserByUserName(name);

		Pageable pageable = PageRequest.of(page, 4);
		Page<Contact> contacts = contactRepository.findContactsByUser(user.getId(), pageable);

		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/add_contact_form";

	}
	
	@GetMapping("/show-contacts/{page}")
	public String showContact(@PathVariable("page") Integer page, Model m, Principal principal) {
		m.addAttribute("title", "Show user contact");
		String name = principal.getName();
		User user = userRepository.getUserByUserName(name);

		Pageable pageable = PageRequest.of(page, 4);
		Page<Contact> contacts = contactRepository.findContactsByUser(user.getId(), pageable);

		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";

	}

	// showing specific contact details

	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model m, Principal principal) {
		Optional<Contact> contact = contactRepository.findById(cId);
		Contact contact2 = contact.get();

		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);

		if (user.getId() == contact2.getUser().getId()) {
			m.addAttribute("contact", contact2);
			m.addAttribute("title", contact2.getName());
		}
		return "normal/contact_detail";
	}

	// delete contact handler
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, Model model, HttpSession session,Principal principal) {
		System.out.println("inside delete");
		Optional<Contact> conOptional = contactRepository.findById(cId);
		Contact contact = conOptional.get();

		
		User user = userRepository.getUserByUserName(principal.getName());
		user.getContacts().remove(contact);
		this.userRepository.save(user);
		//contactRepository.delete(contact);
		session.setAttribute("message", new Message("Contact deleted successfully", "success"));
		return "redirect:/user/show-contacts/0";
	}

	// open update form handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model m) {
		m.addAttribute("title", "update Contact");
		Contact contact = contactRepository.findById(cid).get();

		m.addAttribute("contact", contact);
		return "normal/update_form";

	}

	// update contact handler
	@PostMapping("/process-update")
	public String updateMapping(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model m, HttpSession session,Principal principal) {
		try {
				
			//old contact details
			
		 Contact oldContact	=contactRepository.findById(contact.getcId()).get();
			if(!file.isEmpty())
			{
			
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1=new File(deleteFile, oldContact.getImage());
				file1.delete();
				
				File file2 = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(file2.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
			}
			else {
				contact.setImage(oldContact.getImage());
			}
			User user=userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			contactRepository.save(contact);
			session.setAttribute("message",new Message("Your contact is updated...", "success"));
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		System.out.println("CONTACT NAME" + contact.getName());

		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	//your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model)
	{
		model.addAttribute("title","Profile Page");
		return "normal/profile";
	}
	
	
//open settingss handler
	@GetMapping("/settings")
	public String openSettings()
	{
		return "normal/settings";
	}
	
	//change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,@RequestParam("newPassword") String newPassword,Principal principal, HttpSession session)
	{
		System.out.println("old password"+oldPassword);
		System.out.println("new password"+newPassword);
		
		String userName = principal.getName();
		User currentUser = userRepository.getUserByUserName(userName);
		
		if(bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword()))
		{
			currentUser.setPassword(bCryptPasswordEncoder.encode(newPassword));
			userRepository.save(currentUser);
			session.setAttribute("message",new Message("Your Password is successfully changed...", "success"));
		}
		else {
		
			session.setAttribute("message",new Message("Please Enter correct old password!!!!", "danger"));
			return "redirect:/user/settings";
		}
		return "redirect:/user/index";
	}
}
