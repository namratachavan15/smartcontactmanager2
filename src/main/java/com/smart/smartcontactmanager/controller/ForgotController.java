package com.smart.smartcontactmanager.controller;

import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.smartcontactmanager.Repo.UserRepository;
import com.smart.smartcontactmanager.entities.User;
import com.smart.smartcontactmanager.services.EmailService;

@Controller
public class ForgotController {

	Random random = new Random(1000);

	@Autowired
	private EmailService emailService;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	// email id form open

	@RequestMapping("/forgot")
	public String openEmailForm() {
		return "forgot_email_form";
	}

	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email, HttpSession session) {
		System.out.println("EMAIL" + email);
		// generating otp of digit

		int otp = random.nextInt(999999);
		System.out.println("OTP" + otp);

		// write code for otp to email
		String subject = "OTP from SCM";
		String message = "" + "<div style='border:1px solid #e2e2e2; padding:20px'>" + "<h1>" + "OPT is" + "<b>" + otp
				+ "</n>" + "</h1>" + "</div>";
		String to = email;

		boolean flag = emailService.sendEmail(subject, message, to);

		if (flag) {
			session.setAttribute("myotp", otp);
			session.setAttribute("email", email);
			return "verify_otp";
		} else {
			session.setAttribute("message", "Check your email id!!!");
			return "forgot_email_form";
		}

	}

	@PostMapping("/verify-otp")

	public String verfifyOtp(@RequestParam("otp") int otp, HttpSession session) {
		int myopt = (int) session.getAttribute("myotp");
		String email = (String) session.getAttribute("email");
		if (myopt == otp) {
			User user = userRepository.getUserByUserName(email);
			if (user == null) {
				// send error msg
				session.setAttribute("message", "User does not exits with this email!!!");
				return "forgot_email_form";
			}

			else {

				// send chnage password form

			}
			// password change form
			return "password_change_form";
		} else {

			session.setAttribute("message", "you have entered wrong otp");
			return "verify_otp";
		}
	}
	
	//change password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newpassword") String newpassword,HttpSession session)
	{
		String email=(String)session.getAttribute("email");
		User user=userRepository.getUserByUserName(email);
		user.setPassword(bCryptPasswordEncoder.encode(newpassword));
		userRepository.save(user);
		
		
		return "redirect:/signin?change=password changged successfully";
	}
}
