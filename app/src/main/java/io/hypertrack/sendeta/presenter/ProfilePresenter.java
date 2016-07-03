package io.hypertrack.sendeta.presenter;

import android.graphics.Bitmap;
import android.text.TextUtils;

import java.io.File;

import io.hypertrack.sendeta.interactor.ProfileInteractor;
import io.hypertrack.sendeta.interactor.callback.OnProfilePicUploadCallback;
import io.hypertrack.sendeta.interactor.callback.OnProfileUpdateCallback;
import io.hypertrack.sendeta.model.OnboardingUser;
import io.hypertrack.sendeta.store.AnalyticsStore;
import io.hypertrack.sendeta.store.OnboardingManager;
import io.hypertrack.sendeta.util.ErrorMessages;
import io.hypertrack.sendeta.view.ProfileView;

/**
 * Created by suhas on 24/02/16.
 */
public class ProfilePresenter implements IProfilePresenter<ProfileView> {

    private ProfileView view;
    private ProfileInteractor profileInteractor;
    private OnboardingManager onboardingManager = OnboardingManager.sharedManager();

    @Override
    public void attachView(ProfileView view) {
        this.view = view;
        profileInteractor = new ProfileInteractor();

        OnboardingUser user = this.onboardingManager.getUser();
        this.view.updateViews(user.getFirstName(), user.getLastName(), user.getPhotoURL());
    }

    @Override
    public void detachView() {
        view = null;
    }

    @Override
    public void attemptLogin(String userFirstName, String userLastName, final File profileImage,
                             final Bitmap oldProfileImage, final Bitmap updatedProfileImage) {

        if (TextUtils.isEmpty(userFirstName)) {
            if (view != null) {
                view.showFirstNameValidationError();
            }
            return;
        }

        if (TextUtils.isEmpty(userLastName)) {
            if (view != null) {
                view.showLastNameValidationError();
            }
            return;
        }

        final OnboardingUser user = this.onboardingManager.getUser();
        user.setFirstName(userFirstName);
        user.setLastName(userLastName);

        if (profileImage != null && profileImage.length() > 0) {
            user.setPhotoImage(profileImage);
        }


        profileInteractor.updateUserProfile(new OnProfileUpdateCallback() {
            @Override
            public void OnSuccess() {
                AnalyticsStore.getLogger().enteredName(true, null);
                AnalyticsStore.getLogger().completedProfileSetUp(user.isExistingUser());

                // Check if the profile image has changed from the existing one
                if (updatedProfileImage != null && updatedProfileImage.getByteCount() > 0 && !updatedProfileImage.sameAs(oldProfileImage)) {

                    profileInteractor.updateUserProfilePic(new OnProfilePicUploadCallback() {

                        @Override
                        public void OnSuccess() {

                            if (view != null) {
                                view.showProfilePicUploadSuccess();
                            }

                            AnalyticsStore.getLogger().uploadedProfilePhoto(true, null);
                        }

                        @Override
                        public void OnError() {

                            if (view != null) {
                                view.showProfilePicUploadError();
                            }

                            AnalyticsStore.getLogger().uploadedProfilePhoto(false,
                                    ErrorMessages.PROFILE_PIC_UPLOAD_FAILED);
                        }

                    });
                } else {
                    // Signup Completed as Profile Image need not be uploaded
                    if (view != null) {
                        view.navigateToHomeScreen();
                    }
                }
            }

            @Override
            public void OnError() {
                if (view != null) {
                    view.showErrorMessage();
                }

                AnalyticsStore.getLogger().enteredName(false, ErrorMessages.PROFILE_UPDATE_FAILED);
            }
        });
    }
}
