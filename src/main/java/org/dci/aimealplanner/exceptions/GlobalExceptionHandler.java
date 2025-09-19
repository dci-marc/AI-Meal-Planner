package org.dci.aimealplanner.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dci.aimealplanner.services.users.UserInformationService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final UserInformationService userInformationService;

    private ModelAndView errorPage(HttpStatus status, String message, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("error/error");
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", (message == null || message.isBlank()) ? "An error occurred." : message);
        mav.addObject("path", request.getRequestURI());

        var principal = request.getUserPrincipal();
        if (principal instanceof org.springframework.security.core.Authentication auth && auth.isAuthenticated()) {
            mav.addObject("loggedInUser", userInformationService.getUserBasicDTO(auth));
        }

        mav.setStatus(status);
        return mav;
    }

    @ExceptionHandler({
            IngredientNotFoundException.class,
            MealCategoryNotFoundException.class,
            MealEntryNotFoundException.class,
            RecipeNotFoundException.class,
            UnitNotFoundException.class,
            UnitRatioNotFoundException.class,
            UserNotFoundException.class
    })
    public ModelAndView handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return errorPage(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({
            InvalidIngredientException.class,
            InvalidUnitCodeException.class,
            VerificationTokenInvalidException.class,
            PasswordInvalidException.class,
            IllegalArgumentException.class
    })
    public ModelAndView handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return errorPage(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(EmailAlreadyTakenException.class)
    public ModelAndView handleConflict(RuntimeException ex, HttpServletRequest request) {
        return errorPage(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAll(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return errorPage(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong.", request);
    }
}
