package com.csvprocessor.aspect;

import java.util.HashMap;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class LoggingAspect {
	private static Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
	HashMap<String, Long> timeTaken = new HashMap<>();
	
	@Before("execution(* com.csvprocessor.controller.ChatController..*(..))")
	public void beforeController(JoinPoint joinPoint) {
		timeTaken.put("StartTime", System.currentTimeMillis());
		logger.info("Before: " + joinPoint.getSignature().toShortString());
	}

	@After("execution(* com.csvprocessor.controller.ChatController..*(..))")
	public void afterController(JoinPoint joinPoint) {
		timeTaken.put("EndTime", System.currentTimeMillis());
		logger.info("API execution time: {}", timeTaken.get("EndTime")-timeTaken.get("StartTime"));
		logger.info("After: " + joinPoint.getSignature().toShortString());
	}

} 
