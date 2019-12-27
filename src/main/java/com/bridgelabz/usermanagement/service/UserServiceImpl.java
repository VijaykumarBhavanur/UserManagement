package com.bridgelabz.usermanagement.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.bridgelabz.usermanagement.dto.RegisterDTO;
import com.bridgelabz.usermanagement.exception.LoginException;
import com.bridgelabz.usermanagement.exception.RegistrationException;
import com.bridgelabz.usermanagement.exception.UnautorizedException;
import com.bridgelabz.usermanagement.model.User;
import com.bridgelabz.usermanagement.repository.IRegisterRepository;
import com.bridgelabz.usermanagement.response.Response;
import com.bridgelabz.usermanagement.util.TokenUtil;
import com.bridgelabz.usermanagement.util.Utility;

@Service
public class UserServiceImpl implements IUserService {

	@Autowired
	private IRegisterRepository regRepository;

	@Autowired(required = true)
	private JavaMailSender javaMailSender;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	public Response validateCredentials(String email, String password) {
		if (email.isEmpty() || password.isEmpty())
			throw new LoginException("Please enter both fields!!");
		User user = regRepository.findByEmailId(email);
		if (user == null)
			throw new LoginException("Invalid EmailId");
		boolean result = bCryptPasswordEncoder.matches(password, user.getPassword());

		if (result)
			return new Response(HttpStatus.OK, null, "Login sucess");
		throw new UnautorizedException("Unauthorized User");
	}

	public Response registerUser(RegisterDTO regdto) {

		if (regRepository.findByEmailId(regdto.getEmailId()) != null) {
			throw new RegistrationException("EmailId already exist!!");
		}

		User regUser = modelMapper.map(regdto, User.class);
		regUser.setPassword(bCryptPasswordEncoder.encode(regdto.getPassword()));
		regUser.setConfirmPassword(bCryptPasswordEncoder.encode(regdto.getConfirmPassword()));
		sendEmail(regdto.getEmailId(), TokenUtil.getJWTToken(regdto.getEmailId()));

		regRepository.save(regUser);

		return new Response(HttpStatus.OK, null, "success");

	}

	public Response sendEmail(String email, String token) {
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setTo(email);
		mail.setSubject("Testing Mail API");
		mail.setText("Use this token to change password ===>  " + token);
		javaMailSender.send(mail);
		return new Response(HttpStatus.OK, null, "Mail sent successfully...");

	}

	@Override
	public String getJWTToken(String email) {
		return TokenUtil.getJWTToken(email);
	}

	@Override
	public Response saveProfilePic(MultipartFile file, String emailId) throws Exception {
		User user = regRepository.findByEmailId(emailId);
		if (user == null)
			throw new UnautorizedException("Unautorized User");

		byte[] bytes = file.getBytes();
		String extension = file.getContentType().replace("image/", "");
		String fileLocation = Utility.PROFILE_PIC_LOCATION + emailId + "." + extension;
		Path path = Paths.get(fileLocation);
		Files.write(path, bytes);

		user.setProfilePic(fileLocation);
		regRepository.save(user);
		return new Response(HttpStatus.OK, null, Utility.RECORD_UPDATED);
	}

	@Override
	public List<User> getUsers() {
		return regRepository.findAll();
	}

	@Override
	public Response deleteProfilePic(String emailId) {
		User register = regRepository.findByEmailId(emailId);
		if (register == null)
			throw new UnautorizedException("Unautorized User");

		String fileLocation = register.getProfilePic();
		File file = new File(fileLocation);
		file.delete();
		register.setProfilePic("");
		regRepository.save(register);
		return new Response(HttpStatus.OK, null, Utility.RECORD_UPDATED);
	}

	public Response updateProfilePic(MultipartFile file, String emailId) throws IOException {

		User register = regRepository.findByEmailId(emailId);
		if (register == null)
			throw new UnautorizedException("Unautorized User");

		byte[] bytes = file.getBytes();
		String extension = file.getContentType().replace("image/", "");
		String fileLocation = Utility.PROFILE_PIC_LOCATION + emailId + "." + extension;
		Path path = Paths.get(fileLocation);
		Files.write(path, bytes);

		register.setProfilePic(fileLocation);
		regRepository.save(register);
		return new Response(HttpStatus.OK, null, Utility.RECORD_UPDATED);

	}

	@Override
	public Response verifyUser(String email) {
		User user = regRepository.findByEmailId(email);
		if (user == null)
			throw new UnautorizedException("Unautorized user");
		regRepository.save(user);
		return new Response(HttpStatus.OK, null, Utility.RECORD_UPDATED);
	}

}
