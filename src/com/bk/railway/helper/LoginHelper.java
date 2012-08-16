package com.bk.railway.helper;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class LoginHelper {

    public static String getUsername(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        
        UserService userService = UserServiceFactory.getUserService();

        String thisURL = req.getRequestURI();

        resp.setContentType("text/html");
        if (req.getUserPrincipal() != null) {
            return req.getUserPrincipal().getName();
        } else {
            resp.sendRedirect(userService.createLoginURL(thisURL));
            throw new IOException("Need Login");
        }
    }
    
}
