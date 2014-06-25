package br.com.drfacil.android.fragments.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import br.com.drfacil.android.R;
import br.com.drfacil.android.endpoints.ApiManager;
import br.com.drfacil.android.endpoints.ProfessionalApi;
import br.com.drfacil.android.ext.image.UrlImageView;
import br.com.drfacil.android.ext.instance.InstanceFactory;
import br.com.drfacil.android.ext.instance.LazyWeakFactory;
import br.com.drfacil.android.fragments.profile.infos.ProfileInfoSpecialtyFragment;
import br.com.drfacil.android.helpers.CustomViewHelper;
import br.com.drfacil.android.model.Professional;
import br.com.drfacil.android.model.Slot;
import org.joda.time.DateTime;

import java.util.List;

public class ProfileFragment extends Fragment {

    private UrlImageView vImage;
    private ListView vSchedule;
    private TextView vSpecialty;
    private TextView vEmail;
    private TextView vPhone;
    private View vProgressBar;

    private Professional mProfessional;
    private ProfileScheduleAdapter mScheduleAdapter;
    private DateTime mScheduleStartDate = DateTime.now();
    private DateTime mScheduleEndDate = DateTime.now().plusDays(5);
    private InstanceFactory<ProfileInfoSpecialtyFragment> mSpecialtyFragmentFactory;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vImage = (UrlImageView) getView().findViewById(R.id.profile_image);
        vSchedule = (ListView) getView().findViewById(R.id.profile_schedule);
        mScheduleAdapter = new ProfileScheduleAdapter(getActivity());
        vSchedule.setAdapter(mScheduleAdapter);
        vProgressBar = getView().findViewById(R.id.profile_progress_spinner);
        vSpecialty = (TextView) getView().findViewById(R.id.profile_specialty);
        ((View) vSpecialty.getParent()).setOnClickListener(mOnSpecialtyInfoClick);
        vEmail = (TextView) getView().findViewById(R.id.profile_email);
        ((View) vEmail.getParent()).setOnClickListener(mOnEmailInfoClick);
        vPhone = (TextView) getView().findViewById(R.id.profile_phone);
        ((View) vPhone.getParent()).setOnClickListener(mOnPhoneInfoClick);
        if (mProfessional != null) tryUpdateView();
    }

    private final View.OnClickListener mOnSpecialtyInfoClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(getActivity(), "Coming Soon", Toast.LENGTH_LONG).show();
            ProfileInfoSpecialtyFragment fragment =
                    mSpecialtyFragmentFactory.getInstance(ProfileInfoSpecialtyFragment.class);
            fragment.show(getFragmentManager(), ((Object) fragment).getClass().toString());
        }
    };

    private final View.OnClickListener mOnEmailInfoClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_EMAIL, mProfessional.getEmail());
            startActivity(Intent.createChooser(intent, getResources().getString(R.string.send_email_label)));
        }
    };

    private final View.OnClickListener mOnPhoneInfoClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = mProfessional.getPhone().replaceAll("[^\\d]", "");
            intent.setData(Uri.parse("tel:" + phone));
            startActivity(intent);
        }
    };

    public ProfileFragment setProfessional(Professional professional) {
        mProfessional = professional;
        mSpecialtyFragmentFactory = new LazyWeakFactory.WithFixedArgumentBuilder()
                .argument(Professional.class, mProfessional)
                .build();
        return this;
    }

    public ProfileFragment setScheduleStartDate(DateTime scheduleStartDate) {
        mScheduleStartDate = scheduleStartDate;
        return this;
    }

    public ProfileFragment setScheduleEndDate(DateTime scheduleEndDate) {
        mScheduleEndDate = scheduleEndDate;
        return this;
    }

    public void tryUpdateView() {
        if (getView() == null) return;
        getActivity().setTitle(mProfessional.getName());
        vImage.setUrl(mProfessional.getImageUrl());
        vSpecialty.setText(mProfessional.getSpecialty().toString());
        vEmail.setText(mProfessional.getEmail());
        vPhone.setText(mProfessional.getPhone());
        new FetchSlotsTask().execute();
    }

    private class FetchSlotsTask extends AsyncTask<Void, Void, List<Slot>> {

        @Override
        protected void onPreExecute() {
            CustomViewHelper.toggleVisibleGone(vProgressBar, true);
        }

        @Override
        protected List<Slot> doInBackground(Void... params) {
            ApiManager apiManager = ApiManager.getInstance(getActivity());
            ProfessionalApi api = apiManager.getApi(ProfessionalApi.class);
            return api.slots(mProfessional.getId(), mScheduleStartDate, mScheduleEndDate);
        }

        @Override
        protected void onPostExecute(List<Slot> slots) {
            CustomViewHelper.toggleVisibleGone(vProgressBar, false);
            mScheduleAdapter.update(slots);
        }
    }
}
