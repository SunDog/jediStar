package fr.jedistar.commands;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vdurmont.emoji.EmojiManager;

import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.message.embed.EmbedBuilder;
import de.btobastian.javacord.entities.message.impl.ImplReaction;
import fr.jedistar.JediStarBotCommand;
import fr.jedistar.StaticVars;
import fr.jedistar.formats.CommandAnswer;
import fr.jedistar.formats.PendingAction;
import fr.jedistar.listener.JediStarBotReactionAddListener;

public class PayoutCommand implements JediStarBotCommand {

	final static Logger logger = LoggerFactory.getLogger(PayoutCommand.class);

	private final String COMMAND;
	private final String COMMAND_ADD;
	private final String COMMAND_DELETE;
	
	private final String HELP;
	
	private final String MESSAGE_CONFIRM_TIMEZONE;
	private final String MESSAGE_TIMEZONE_CHOICE;
	private final String MESSAGE_TIMEZONE_NOT_FOUND;
	private final String MESSAGE_CANCEL;
	private final String MESSAGE_ADD_USER_SUCCESS;
	private final String MESSAGE_CONFIRM_DELETE;
	private final String MESSAGE_DELETE_SUCCESS;
	private final String EMBED_TITLE;
	
	private final String FORBIDDEN;
	private final String ERROR_MESSAGE;
	private final String ERROR_UNRECOGNIZED_COMMAND;
	private final String ERROR_TIME_FORMAT;
	private final String SQL_ERROR;
	private final String ERROR_NO_CHANNEL;
	private final String ERROR_USER_NOT_FOUND;

	private final static Color EMBED_COLOR = Color.YELLOW;
	private final static String CLOCK_IMG_URL = "http://37.187.39.193/swgoh/clock.png";
	//SQL
	private final static String SQL_INSERT_USER = "INSERT INTO payoutTime(channelID,userName,payoutTime,flag,swgohggLink) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE payoutTime=VALUES(payoutTime),flag=VALUES(flag),swgohggLink=VALUES(swgohggLink);";
	private final static String SQL_SELECT_USER = "SELECT * FROM payoutTime WHERE channelID=? AND userName=?";
	private final static String SQL_SELECT_CHANNEL_USERS = "SELECT * FROM payoutTime WHERE channelID=? ORDER BY payoutTime,userName";
	private final static String SQL_DELETE_USER = "DELETE FROM payoutTime WHERE channelID=? AND userName=?";

	//Json field names
	private final static String JSON_ERROR_MESSAGE = "errorMessage";

	private final static String JSON_PAYOUT = "payoutCommandParameters";
	
	private final static String JSON_PAYOUT_HELP = "help";

	private final static String JSON_PAYOUT_COMMANDS = "commands";
	private final static String JSON_PAYOUT_COMMANDS_MAIN = "main";
	private final static String JSON_PAYOUT_COMMANDS_ADD = "add";
	private final static String JSON_PAYOUT_COMMANDS_DELETE = "delete";
	
	private final static String JSON_PAYOUT_MESSAGES = "messages";
	private final static String JSON_PAYOUT_MESSAGES_TIMEZONE_CONFIRMATION = "timezoneConfirmation";
	private final static String JSON_PAYOUT_MESSAGES_TIMEZONE_CHOICE = "timezoneChoice";
	private final static String JSON_PAYOUT_MESSAGES_TIMEZONE_NOT_FOUND = "noTimezoneFound";
	private final static String JSON_PAYOUT_MESSAGES_CANCEL = "cancel";
	private final static String JSON_PAYOUT_MESSAGES_ADD_USER_SUCCESS = "addUserSuccess";
	private final static String JSON_PAYOUT_MESSAGES_CONFIRM_DELETE = "deleteConfirmation";
	private final static String JSON_PAYOUT_MESSAGES_DELETE_SUCCESS = "deleteSuccess";
	private final static String JSON_PAYOUT_MESSAGES_EMBED_TITLE = "embedTitle";

	private final static String JSON_PAYOUT_ERRORS = "errorMessages";
	private final static String JSON_PAYOUT_ERRORS_FORBIDDEN = "forbidden";
	private final static String JSON_PAYOUT_ERRORS_UNRECOGNIZED_COMMAND = "unrecognizedCommand";
	private final static String JSON_PAYOUT_ERRORS_TIME_FORMAT = "timeFormat";
	private final static String JSON_PAYOUT_ERRORS_SQL = "sqlError";
	private final static String JSON_PAYOUT_ERRORS_NO_CHANNEL = "noChannel";
	private final static String JSON_PAYOUT_ERRORS_USER_NOT_FOUND = "noUserFound";


	public PayoutCommand() {
		JSONObject parameters = StaticVars.jsonSettings;

		ERROR_MESSAGE = parameters.getString(JSON_ERROR_MESSAGE);
		
		JSONObject payoutParams = parameters.getJSONObject(JSON_PAYOUT);
		
		HELP = payoutParams.getString(JSON_PAYOUT_HELP);

		JSONObject commands = payoutParams.getJSONObject(JSON_PAYOUT_COMMANDS);
		COMMAND = commands.getString(JSON_PAYOUT_COMMANDS_MAIN);
		COMMAND_ADD  = commands.getString(JSON_PAYOUT_COMMANDS_ADD);
		COMMAND_DELETE = commands.getString(JSON_PAYOUT_COMMANDS_DELETE);
		
		JSONObject messages = payoutParams.getJSONObject(JSON_PAYOUT_MESSAGES);
		MESSAGE_CONFIRM_TIMEZONE = messages.getString(JSON_PAYOUT_MESSAGES_TIMEZONE_CONFIRMATION);
		MESSAGE_TIMEZONE_CHOICE = messages.getString(JSON_PAYOUT_MESSAGES_TIMEZONE_CHOICE);
		MESSAGE_TIMEZONE_NOT_FOUND = messages.getString(JSON_PAYOUT_MESSAGES_TIMEZONE_NOT_FOUND);
		MESSAGE_CANCEL = messages.getString(JSON_PAYOUT_MESSAGES_CANCEL);
		MESSAGE_ADD_USER_SUCCESS = messages.getString(JSON_PAYOUT_MESSAGES_ADD_USER_SUCCESS);
		MESSAGE_CONFIRM_DELETE = messages.getString(JSON_PAYOUT_MESSAGES_CONFIRM_DELETE);
		MESSAGE_DELETE_SUCCESS = messages.getString(JSON_PAYOUT_MESSAGES_DELETE_SUCCESS);
		EMBED_TITLE = messages.getString(JSON_PAYOUT_MESSAGES_EMBED_TITLE);
		
		JSONObject errorMessages = payoutParams.getJSONObject(JSON_PAYOUT_ERRORS);
		ERROR_UNRECOGNIZED_COMMAND = errorMessages.getString(JSON_PAYOUT_ERRORS_UNRECOGNIZED_COMMAND);
		FORBIDDEN = errorMessages.getString(JSON_PAYOUT_ERRORS_FORBIDDEN);
		ERROR_TIME_FORMAT = errorMessages.getString(JSON_PAYOUT_ERRORS_TIME_FORMAT);
		SQL_ERROR = errorMessages.getString(JSON_PAYOUT_ERRORS_SQL);
		ERROR_NO_CHANNEL = errorMessages.getString(JSON_PAYOUT_ERRORS_NO_CHANNEL);
		ERROR_USER_NOT_FOUND = errorMessages.getString(JSON_PAYOUT_ERRORS_USER_NOT_FOUND);
	}
	
	@Override
	public String getCommand() {
		return COMMAND;
	}

	@Override
	public CommandAnswer answer(List<String> params, Message receivedMessage, boolean isAdmin) {

		if(receivedMessage.isPrivateMessage() || receivedMessage.getChannelReceiver() == null) {
			return error(ERROR_NO_CHANNEL);
		}
		
		if(params.size() == 0) {
			return formatPayouts(receivedMessage.getChannelReceiver().getId());
		}
		
		if(params.size() >=4 && params.size()<=6 && COMMAND_ADD.equals(params.get(0))) {
			if(!isAdmin) {
				return new CommandAnswer(FORBIDDEN,null);
			}
			
			return beforeAddUser(params,receivedMessage);
		}
		
		if(params.size()>=2 && COMMAND_DELETE.equals(params.get(0))) {
			if(!isAdmin) {
				return new CommandAnswer(FORBIDDEN,null);
			}
			
			return beforeDeleteUser(receivedMessage,params.get(1));
		}
		
		
		return error(ERROR_UNRECOGNIZED_COMMAND);
	}

	
	private CommandAnswer formatPayouts(String channelID) {
		
		String[] embedContent = new String[24];
		
		TimeZone utc = TimeZone.getTimeZone("UTC");
		Calendar now = getCalendarWithoutDate(utc);
		
		EmbedBuilder embed = new EmbedBuilder();
		embed.setThumbnail(CLOCK_IMG_URL);
		embed.setColor(EMBED_COLOR);
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			conn = StaticVars.getJdbcConnection();
			
			stmt = conn.prepareStatement(SQL_SELECT_CHANNEL_USERS);
			
			stmt.setString(1, channelID);
			
			logger.info("executingQuery "+stmt.toString());
			rs = stmt.executeQuery();
			
			while(rs.next()) {
				String userName = rs.getString("userName");
				Calendar payoutTime = Calendar.getInstance(utc);
				payoutTime.setTime(rs.getTime("payoutTime"));
				String flag = rs.getString("flag");
				String swgohggLink = rs.getString("swgohggLink");
				
				if(payoutTime.before(now)) {
					payoutTime.add(Calendar.DAY_OF_MONTH, 1);
				}
				
				Long difference = payoutTime.getTimeInMillis() - now.getTimeInMillis();
				
				Long hoursDifference = difference / (60 * 60 * 1000) %24;
				Long minutesDifference = difference / (60 * 1000) %60;
				
				int index = (int)hoursDifference.longValue();
				
				String contentLine = embedContent[index];
				
				if(contentLine == null) {
					
					
					String strHours = StringUtils.leftPad(hoursDifference.toString(), 2, "0");
					String strMinutes = StringUtils.leftPad(minutesDifference.toString(), 2, "0");
					
					contentLine = "`"+strHours+":"+strMinutes+"` ";
				}
				
				boolean hasLink = StringUtils.isNotBlank(swgohggLink);
				
				StringBuilder builder = new StringBuilder();
				builder.append(flag);	
				if(hasLink) {
					builder.append('[');
				}
				builder.append(userName);
				if(hasLink) {
					builder.append("](");
					builder.append(swgohggLink);
					builder.append(")");
				}
				builder.append(' ');
				contentLine += builder.toString();	
				
				embedContent[index] =  contentLine;
			}
			
			String content = "";
			for(String contentLine : embedContent) {
				if(StringUtils.isNotBlank(contentLine)) {
					content += contentLine + "\r\n";
				}
			}
			
			embed.addField(EMBED_TITLE, content,false);
			
			return new CommandAnswer(null,embed);
		}
		catch(SQLException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return new CommandAnswer(SQL_ERROR,null);
		}
		finally {
			if(rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					logger.error(e.getMessage());
				}
			}
			if(stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	private CommandAnswer beforeDeleteUser(Message receivedMessage,String userName) {
		String emojiX = EmojiManager.getForAlias("x").getUnicode();
		String emojiV = EmojiManager.getForAlias("white_check_mark").getUnicode();
		
		String channelID = receivedMessage.getChannelReceiver().getId();

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			conn = StaticVars.getJdbcConnection();
			
			stmt = conn.prepareStatement(SQL_SELECT_USER);
			
			stmt.setString(1,channelID);
			stmt.setString(2, userName);
			
			logger.info("executingQuery "+stmt.toString());
			rs = stmt.executeQuery();
			
			if(!rs.next()) {
				return new CommandAnswer(ERROR_USER_NOT_FOUND,null);
			}
			
			PendingAction action = new PendingAction(receivedMessage.getAuthor(),"deleteUser",this,receivedMessage,1,userName,channelID);
			JediStarBotReactionAddListener.addPendingAction(action);
			
			return new CommandAnswer(MESSAGE_CONFIRM_DELETE,null,emojiV,emojiX);
		}
		catch(SQLException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return new CommandAnswer(SQL_ERROR,null);
		}
		finally {
			if(rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					logger.error(e.getMessage());
				}
			}
			if(stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	public String deleteUser(ImplReaction reaction,String userName,String channelID) {
		String emojiV = EmojiManager.getForAlias("white_check_mark").getUnicode();
		
		if(!emojiV.equals(reaction.getUnicodeEmoji())) {
			return MESSAGE_CANCEL;
		}
		
		Connection conn = null;
		PreparedStatement stmt = null;
		
		try {
			conn = StaticVars.getJdbcConnection();
			
			stmt = conn.prepareStatement(SQL_DELETE_USER);
			
			stmt.setString(1, channelID);
			stmt.setString(2, userName);
			
			logger.info("executingQuery "+stmt.toString());
			stmt.executeUpdate();
			
			return MESSAGE_DELETE_SUCCESS;
		}
		catch(SQLException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return SQL_ERROR;
		}
		finally {
			if(stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}
	private CommandAnswer beforeAddUser(List<String> params,Message receivedMessage) {
		
		String userName = params.get(1);
		
		String payoutTime = params.get(2);
		
		Pattern pattern = Pattern.compile("[0-9]{2}:[0-9]{2}");
		Matcher matcher = pattern.matcher(payoutTime);
		
		if(!matcher.matches()) {
			return error(ERROR_TIME_FORMAT);
		}
		
		String flag = "";
		String swgohggLink = "";
		
		if(params.size()>=5) {
			flag = params.get(4);
		}
		else {
			flag = "flag_white";
		}
		if(params.size()>=6) {
			swgohggLink = params.get(5);
		}
		
		String timezoneName = params.get(3);
				
		String matchingTimeZone = null;
		List<String> matchingTimeZones = new ArrayList<String>();
		
		for(String timeZone : TimeZone.getAvailableIDs()) {
			if(timeZone.equalsIgnoreCase(timezoneName)) {
				matchingTimeZone = timeZone;
				break;
			}
			if(StringUtils.containsIgnoreCase(timeZone, timezoneName)) {
				matchingTimeZones.add(timeZone);
			}
		}
		
		if(matchingTimeZone == null && matchingTimeZones.size() == 1) {
			matchingTimeZone = matchingTimeZones.get(0);
		}
			
		if(matchingTimeZone != null) {
			return confirmTimezone(userName,payoutTime,matchingTimeZone,flag,swgohggLink,receivedMessage);
		}
		
		if(matchingTimeZones.size()>1) {
			EmbedBuilder embed = new EmbedBuilder();
			embed.setTitle(MESSAGE_TIMEZONE_CHOICE);
			
			int counter = 10;
			String content = "";
			for(String timeZone : matchingTimeZones) {
				content += timeZone + "\r\n";
				counter --;
				
				if(counter == 0) {
					embed.addField(".", content, true);
					content = "";
					counter = 10;
				}
			}
			
			if(StringUtils.isNotBlank(content)) {
				embed.addField(".", content, true);

			}
			
			return new CommandAnswer(null,embed);
		}
		
		return noMatchingTimezone(receivedMessage);
	}

	
	private CommandAnswer noMatchingTimezone(Message receivedMessage) {
		Path path = Paths.get("timezones.txt");
		
		String timezones = "";
		for(String tz : TimeZone.getAvailableIDs()) {
			timezones += tz+"\r\n";
		}
		
		try {
			Files.write(path,timezones.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
			logger.warn(e.getMessage());
		}
		
		receivedMessage.getChannelReceiver().sendFile(path.toFile(), MESSAGE_TIMEZONE_NOT_FOUND);
		
		return null;
	}

	private CommandAnswer confirmTimezone(String userName,String payoutTime, String timezoneName, String flag, String swgohggLink,Message receivedMessage) {

		String emojiX = EmojiManager.getForAlias("x").getUnicode();
		String emojiV = EmojiManager.getForAlias("white_check_mark").getUnicode();
			
		TimeZone timezone = TimeZone.getTimeZone(timezoneName);
		
		Calendar now = getCalendarWithoutDate(timezone);		
		
		Calendar payoutCalendar = getPayoutTime(payoutTime,timezone);
		
		GregorianCalendar payoutCalendar2 = new GregorianCalendar(payoutCalendar.getTimeZone());
		payoutCalendar2.setTimeInMillis(payoutCalendar.getTimeInMillis());
		
		if(payoutCalendar.before(now)) {
			payoutCalendar.add(Calendar.DAY_OF_MONTH, 1);
			
		}
		Long difference = payoutCalendar.getTimeInMillis() - now.getTimeInMillis();
		
		Long hoursDifference = difference / (60 * 60 * 1000) %24;
		Long minutesDifference = difference / (60 * 1000 ) %60;
				
		String message = String.format(MESSAGE_CONFIRM_TIMEZONE, timezoneName,hoursDifference,minutesDifference);
		
		PendingAction action = new PendingAction(receivedMessage.getAuthor(),"addUser",this, receivedMessage,5, userName,payoutCalendar2,flag,swgohggLink,receivedMessage.getChannelReceiver().getId());
		JediStarBotReactionAddListener.addPendingAction(action);
		
		return new CommandAnswer(message,null,emojiV,emojiX);
		
	}
	
	public String addUser(ImplReaction react,String userName,GregorianCalendar payoutCalendar,String flag,String swgohggLink,String channelID) {
		
		String emojiV = EmojiManager.getForAlias("white_check_mark").getUnicode();
		
		long timeMillisUTC = (long) payoutCalendar.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000;
		timeMillisUTC = timeMillisUTC + (long) payoutCalendar.get(Calendar.MINUTE) * 60 * 1000;
		timeMillisUTC = timeMillisUTC - (long) payoutCalendar.getTimeZone().getOffset(System.currentTimeMillis());
		
		if(!emojiV.equals(react.getUnicodeEmoji())) {
			return MESSAGE_CANCEL;
		}

		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = StaticVars.getJdbcConnection();
			
			stmt = conn.prepareStatement(SQL_INSERT_USER);
			
			stmt.setString(1,channelID);
			stmt.setString(2,userName);
			stmt.setTime(3, new Time(timeMillisUTC));
			stmt.setString(4, ":"+flag+":");
			stmt.setString(5, swgohggLink);
			
			logger.info("executingQuery "+stmt.toString());
			stmt.executeUpdate();
			
			return MESSAGE_ADD_USER_SUCCESS;
		}
		catch(SQLException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return SQL_ERROR;
		}
		finally {
			
			if(stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	/**
	 * Instanciates a Calendar from a HH:MM String
	 * @param string
	 * @return
	 */
	private Calendar getPayoutTime(String string,TimeZone timezone) {
		
		Pattern pattern = Pattern.compile("[0-9]{2}:[0-9]{2}");
		Matcher matcher = pattern.matcher(string);
		
		if(!matcher.matches()) {
			return null;
		}
		
		String[] split = string.split(":");
		
		Integer hours = Integer.parseInt(split[0]);
		Integer	minutes = Integer.parseInt(split[1]);
				
		Calendar calendar = Calendar.getInstance(timezone);
		
		calendar.setTimeInMillis(0);
		
		calendar.setTimeZone(timezone);
		
		calendar.set(Calendar.HOUR_OF_DAY, hours);
		calendar.set(Calendar.MINUTE,minutes);
		
		calendar.getTimeInMillis();
		
		return calendar;
	}

	/**
	 * Instanciates a Calendar object with all the fields other than hour and minutes set to zero with the UTC timeZone
	 * @return
	 */
	private GregorianCalendar getCalendarWithoutDate(TimeZone tz) {

		GregorianCalendar calendar = new GregorianCalendar(tz);

		Integer hours = calendar.get(Calendar.HOUR_OF_DAY);
		Integer minutes = calendar.get(Calendar.MINUTE);

		calendar.setTimeInMillis(0);

		calendar.set(Calendar.HOUR_OF_DAY, hours);
		calendar.set(Calendar.MINUTE,minutes);

		calendar.getTimeInMillis();
		return calendar;
	}
	
	private CommandAnswer error(String message) {
		return new CommandAnswer(ERROR_MESSAGE +"**"+ message + "**\r\n\r\n"+ HELP,null);
	}
}
