package com.example.demo.Service.UserServices;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.example.demo.Model.*;
import com.example.demo.Repository.*;
import com.example.demo.Service.Admin.LoginRequest;
import com.example.demo.Service.OtpMailService.SMTP_mailService;
import com.example.demo.Service.StorageService;
import com.example.demo.config.CustomUserDetailsService;
import jakarta.mail.MessagingException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService {
	@Autowired
	private UserRepository userRepo;
	@Autowired
	private CustomUserDetailsService customerUserDetails;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	@Autowired
	private AdminFeedBackRepository adminFeedBackRepository;
	@Autowired
	private UserImageRepository userImageRepository;
	@Autowired
	private UserExtraDetailsRepostiory userExtraDetailsRepostiory;
	@Autowired
	private OrganizerRepository organizerRepository;
	@Autowired
	private ParticipantRepository participantRepository;
	@Autowired
	private StorageService storageService;
	@Autowired
	private SMTP_mailService mailService;

	@Override
	public ResponseEntity<String> uploadPost(Integer userId,String description,MultipartFile post) throws IOException {
		Optional<User> user=userRepo.findById(userId);
		if(user.isPresent()){
			return storageService.addUserPosts(userId,description,post);
		}
		return new ResponseEntity<>("user not found", HttpStatus.NOT_FOUND);
	}

	@Override
	public List<User> getAllUser() {
		return (List<User>) userRepo.findAll(); 
	}

	@Override
	public ResponseEntity<String> updateUserEmail(Integer userId, String userEmail) {
		Optional<User> user=userRepo.findById(userId);
		if(userRepo.findByUserEmail(userEmail).isEmpty()){
				if(user.isPresent()){
					user.get().setUserEmail(userEmail);
					userRepo.save(user.get());
					return new ResponseEntity<>("User email updated successfully",HttpStatus.OK);
				}
				return new ResponseEntity<>("user id not found",HttpStatus.NOT_FOUND);
			}
		else {
			return new ResponseEntity<>("user email is already being used.",HttpStatus.CONFLICT);
		}

}

	@Override
	public ResponseEntity<String> changePassword(Integer userId, String userPassword) {
		Optional<User> user=userRepo.findById(userId);
		if(user.isPresent()){
			user.get().setUserPassword(userPassword);
			userRepo.save(user.get());
			return new ResponseEntity<>("User password updated successfully",HttpStatus.OK);
		}
		return new ResponseEntity<>("user id not found ",HttpStatus.NOT_FOUND);
	}

	@Override
	public ResponseEntity<String> updateUserProfile(Integer userId, MultipartFile file) throws IOException {
		Optional<User> user=userRepo.findById(userId);
		if(user.isPresent()){
			storageService.addUserProfile(userId,file);
			return new ResponseEntity<>("User profile updated",HttpStatus.OK);
		}else {
			return new ResponseEntity<>("userId not found",HttpStatus.NOT_FOUND);
		}

	}

	@Override
	public User getUserById(Integer userId) {
		Optional<User> user=userRepo.findById(userId);
		return user.orElse(null);
	}
	@Override
	public ResponseEntity<?> signinUser(LoginRequest loginRequest) {
		try {
			String email = loginRequest.getEmail();
			String password = loginRequest.getPassword();

			// Authenticate user
			Optional<User> user = userRepo.findByUserEmail(email);
			if (user.isEmpty() || !passwordEncoder.matches(password, user.get().getUserPassword())) {
				// Do not expose specific reason for authentication failure to the client
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect email or password");
			}
			// Clear sensitive information before returning the response

			return ResponseEntity.ok(user);
		} catch (AuthenticationException e) {
			// Handle specific authentication exceptions separately
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect email or password");
		} catch (Exception e) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while signing in");
		}
	}

	@Override
	public ResponseEntity<String> addingFollower(Integer userId, Integer followingId) {
		Optional<UserExtraDetails> userExtraDetails=userExtraDetailsRepostiory.findByUserId(userId);
		Optional<UserExtraDetails> followingExtraDetails=userExtraDetailsRepostiory.findByUserId(followingId);
		if(userExtraDetails.isPresent() && followingExtraDetails.isPresent() && !followingExtraDetails.get().getBlockedList().getBlocked().contains(userId)){
			userExtraDetails.get().getFollowingList().setFollowing(followingId);
			followingExtraDetails.get().getFollowersList().setFollower(userId);
			userExtraDetailsRepostiory.save(followingExtraDetails.get());
			userExtraDetailsRepostiory.save(userExtraDetails.get());
			return new ResponseEntity<>("You started following the fellow traveler",HttpStatus.OK);
		}
		return new ResponseEntity<>("problem on joining",HttpStatus.CONFLICT);
	}

	@Override
	public ResponseEntity<String> blockingFollower(Integer userId, Integer blockingId) {
		Optional<UserExtraDetails> userExtraDetails=userExtraDetailsRepostiory.findByUserId(userId);
		Optional<UserExtraDetails> blockingUserExtraDetails=userExtraDetailsRepostiory.findByUserId(blockingId);
		if(userExtraDetails.isPresent() && blockingUserExtraDetails.isPresent()){
			userExtraDetails.get().getBlockedList().setBlocked(blockingId);
			userExtraDetails.get().getFollowersList().getFollower().remove(blockingId);
			userExtraDetailsRepostiory.save(userExtraDetails.get());
			blockingUserExtraDetails.get().getFollowingList().getFollowing().remove(userId);
			userExtraDetailsRepostiory.save(blockingUserExtraDetails.get());
			return new ResponseEntity<>("blocking done successfully",HttpStatus.OK);
		}
		return new ResponseEntity<>("problem on blocking",HttpStatus.CONFLICT);
	}

	@Override
	public ResponseEntity<String> unBlockingUser(Integer userId, Integer blockedUserId) {
		Optional<UserExtraDetails> userExtraDetails=userExtraDetailsRepostiory.findByUserId(userId);
		if(userExtraDetails.isPresent() && userExtraDetails.get().getBlockedList().getBlocked().contains(blockedUserId)){
			userExtraDetails.get().getBlockedList().getBlocked().remove(blockedUserId);
			userExtraDetailsRepostiory.save(userExtraDetails.get());
			return new ResponseEntity<>("unblocked",HttpStatus.OK);
		}
		return new ResponseEntity<>("problem in unblocking",HttpStatus.CONFLICT);
	}

	@Override
	public List<User> getAllFollowers(Integer userId) {
		Optional<UserExtraDetails> userExtraDetails=userExtraDetailsRepostiory.findByUserId(userId);
		List<User> followersList=new ArrayList<>();
		if(userExtraDetails.isPresent()){
			List<Integer> followersIdList=userExtraDetails.get().getFollowersList().getFollower();
			followersList=followersIdList.stream()
					.map(followersId->userRepo.findById(followersId))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.collect(Collectors.toList());
		}
		return followersList;
	}

	@Override
	public List<User> getAllFollowing(Integer userId) {
		Optional<UserExtraDetails> userExtraDetails=userExtraDetailsRepostiory.findByUserId(userId);
		List<User> followingList=new ArrayList<>();
		if(userExtraDetails.isPresent()){
			List<Integer> followingIdList=userExtraDetails.get().getFollowingList().getFollowing();
			followingList=followingIdList.stream()
					.map(followingId->userRepo.findById(followingId))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.collect(Collectors.toList());
		}
		return followingList;
	}
	@Override
	public String postAdminFeedBack(AdminFeedback feedback) {
		System.out.println(feedback);
		List<User> admin=userRepo.findAllByRole(Role.Admin_Role);
		Optional<User> user=userRepo.findById(feedback.getUserId());
		adminFeedBackRepository.save(feedback);
		if(user.isPresent() && feedback.getIndicate()){
			String Subject="User Feedback";
			String content="User - "+user.get().getUserName()+", Have sent this feedback.\n"+feedback.getFeedBack();
			admin.forEach(adminUser-> {
				try {
					mailService.sendMailService(adminUser.getUserEmail(),Subject,content);
				} catch (MessagingException e) {
					throw new RuntimeException(e);
				}
			});
		}
		return "Feed Back submitted successfully";
	}
	@Override
	public List<User> getAllBlocked(Integer userId) {
		Optional<UserExtraDetails> userExtraDetails=userExtraDetailsRepostiory.findByUserId(userId);
		List<User> blockedList=new ArrayList<>();
		if(userExtraDetails.isPresent()){
			List<Integer> blockedIdList=userExtraDetails.get().getBlockedList().getBlocked();
			blockedList=blockedIdList.stream()
					.map(blockedId->userRepo.findById(blockedId))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.collect(Collectors.toList());
		}
		return blockedList;
	}

	@Override
	public List<Integer> getFollowersId(Integer userId) {
		Optional<UserExtraDetails> userExtraDetails=userExtraDetailsRepostiory.findByUserId(userId);
        return userExtraDetails.map(extraDetails -> extraDetails.getFollowersList().getFollower()).orElse(null);
	}

	@Override
	public List<Integer> getFollowingId(Integer userId) {
		Optional<UserExtraDetails> userExtraDetails=userExtraDetailsRepostiory.findByUserId(userId);
		return userExtraDetails.map(user->user.getFollowingList().getFollowing()).orElse(null);
	}

	@Override
	public List<Integer> getBlockedId(Integer userId) {
		Optional<UserExtraDetails> userExtraDetails=userExtraDetailsRepostiory.findByUserId(userId);
		return userExtraDetails.map(user->user.getBlockedList().getBlocked()).orElse(null);
	}

	@Override
	public ResponseEntity<String> unFollowing(Integer userId, Integer followingId) {
		Optional<UserExtraDetails> userExtraDetails=userExtraDetailsRepostiory.findByUserId(userId);
		Optional<UserExtraDetails> followingUserExtraDetails=userExtraDetailsRepostiory.findByUserId(followingId);
		if(userExtraDetails.isPresent() && followingUserExtraDetails.isPresent() && userExtraDetails.get().getFollowingList().getFollowing().contains(followingId)){
			userExtraDetails.get().getFollowingList().getFollowing().remove(followingId);
			userExtraDetailsRepostiory.save(userExtraDetails.get());
			followingUserExtraDetails.get().getFollowersList().getFollower().remove(userId);
			userExtraDetailsRepostiory.save(followingUserExtraDetails.get());
			return new ResponseEntity<>("Successfully removed from following",HttpStatus.OK);
		}
		return new ResponseEntity<>("Problem on unfollowing",HttpStatus.CONFLICT);
	}

	@Override
	public ResponseEntity<User> updateUser(Integer userId, User updateUser) {
		Optional<User> user=userRepo.findById(userId);
		if(user.isPresent()){
			if(updateUser.getUserName()!=null){
				user.get().setUserName(updateUser.getUserName());
			}
			if(updateUser.getAboutUser()!=null){
				user.get().setAboutUser(updateUser.getAboutUser());
			}
			if(updateUser.getGender()!=null){
				user.get().setGender(updateUser.getGender());
			}
			if(updateUser.getDateOfBirth()!=null){
				user.get().setDateOfBirth(updateUser.getDateOfBirth());
			}
			userRepo.save(user.get());
			Optional<User> user1=userRepo.findById(userId);
			return new ResponseEntity<>(user1.get(),HttpStatus.CREATED);
		}
		return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
	}

	@Override
	public ResponseEntity<Organizer> getOrganizerData(Integer userId) {
		Optional<Organizer> organizer=organizerRepository.findByUserId(userId);
		return new ResponseEntity<>(organizer.orElse(null),HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Participant> getParticipantData(Integer userId) {
		Optional<Participant> participant=participantRepository.findByUserId(userId);
		return new ResponseEntity<>(participant.orElse(null),HttpStatus.OK);
	}

	@Override
	public ResponseEntity<String> addUser(User newUser) {
		Optional<User> user = userRepo.findByUserEmail(newUser.getUserEmail());
		if (user.isPresent()) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("User mail already exists");
		} else {
			newUser.setUserPassword(passwordEncoder.encode(newUser.getUserPassword()));
			userRepo.save(newUser);
			UserImages userImages=new UserImages();
			userImages.setUserId(newUser.getUserId());
			userImageRepository.save(userImages);
			UserExtraDetails extraDetails = new UserExtraDetails();
			extraDetails.setUserId(newUser.getUserId());
			userExtraDetailsRepostiory.save(extraDetails);
			newUser.setUserExtraDetails(extraDetails.getId());
			newUser.setUserProfile(userImages.getId());
			userRepo.save(newUser);
			try {
				String mail = newUser.getUserEmail();
				String subject = "Registration";
				String content = "Hi " + newUser.getUserName() + "\n We are happy to welcome you to be a part of Torry Harris Trip Partner family.";
				mailService.sendMailService(mail, subject, content);
				return ResponseEntity.status(HttpStatus.CREATED).body("User with id: " + newUser.getUserId() + " is registered");
			} catch (MessagingException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@NotNull
	private static UserExtraDetails getUserExtraDetails(User newUser) {
		UserExtraDetails extraDetails=new UserExtraDetails();
		extraDetails.setUserId(newUser.getUserId());
		UserExtraDetails.OrganizedList organizedList=new UserExtraDetails.OrganizedList();
		organizedList.setOrganizedGroupId(List.of(0));
		UserExtraDetails.ParticipatedList participatedList=new UserExtraDetails.ParticipatedList();
		participatedList.setParticipatedGroupId(List.of(0));
		UserExtraDetails.FollowingList followingList=new UserExtraDetails.FollowingList();
		followingList.setFollowing(List.of(0));
		UserExtraDetails.BlockedList blockedList=new UserExtraDetails.BlockedList();
		blockedList.setBlocked(List.of(0));
		UserExtraDetails.FollowersList followersList=new UserExtraDetails.FollowersList();
		followersList.setFollower(List.of(0));
		extraDetails.setOrganizedList(organizedList);
		extraDetails.setParticipatedList(participatedList);
		extraDetails.setBlockedList(blockedList);
		extraDetails.setFollowersList(followersList);
		extraDetails.setFollowingList(followingList);
		return extraDetails;
	}

	@Override
	public String removeUserById(Integer userId) {
		Optional<User> user=userRepo.findById(userId);
		if(user.isPresent()){
			Optional<Organizer> organizer=organizerRepository.findByUserId(userId);
			Optional<Participant> participant=participantRepository.findByUserId(userId);
			Optional<UserExtraDetails> extraDetails=userExtraDetailsRepostiory.findByUserId(userId);
			Optional<UserImages> images=userImageRepository.findByUserId(userId);
			extraDetails.ifPresent(value -> userExtraDetailsRepostiory.delete(value));
			images.ifPresent(value->userImageRepository.delete(value));
            organizer.ifPresent(value -> organizerRepository.delete(value));
            participant.ifPresent(value -> participantRepository.delete((value)));
			userExtraDetailsRepostiory.deleteByUserId(userId);
			userRepo.deleteById(userId);
			return "user with id: "+userId+" is removed successfully";
		}
		else {
			return "user with id: "+userId+" is not found";
		}
	}

	@Override
	public User getByUserEmail(String userEmail) {
		Optional<User> user=userRepo.findByUserEmail(userEmail);
		return user.orElse(null);
	}

	@Override
	public ResponseEntity<String> forgotPassword(String userEmail) {
		Optional<User> user=userRepo.findByUserEmail(userEmail);
		if(user.isPresent()){
			String Password=PasswordGenerator();
			user.get().setUserPassword(passwordEncoder.encode(Password));
			try{
				mailService.sendMailService(userEmail,"Password Changed","Your new Password is : "+Password);
				userRepo.save(user.get());
				return new ResponseEntity<>("New password has been sent to the user's email",HttpStatus.ACCEPTED);
			} catch (MessagingException e) {
				throw new RuntimeException(e);
			}
        }
		else{
			return new ResponseEntity<>("User with this email not found",HttpStatus.NOT_FOUND);
		}

	}

	@Override
	public List<User> getAllByUserName(String userName) {
		return userRepo.findAllByUserName(userName);
	}
	public String PasswordGenerator(){
		String Alpha="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		String Numbers="1234567890";
		String Characters=Alpha+Numbers;
		SecureRandom random=new SecureRandom();
		StringBuilder Password=new StringBuilder();
		for(int i=0;i<8;i++){
			int index=random.nextInt(Characters.length());
			Password.append(Characters.charAt(index));
		}
		return Password.toString();
	}
}
