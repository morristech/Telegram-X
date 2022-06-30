package org.thunderdog.challegram.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.util.DateUtils;
import com.stripe.android.util.StripeTextUtils;

import org.drinkless.td.libcore.telegram.TdApi;
import org.thunderdog.challegram.Log;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.base.SettingView;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.tool.Views;
import org.thunderdog.challegram.widget.MaterialEditTextGroup;

import java.util.ArrayList;
import java.util.List;

import me.vkryl.android.widget.FrameLayoutFix;

public class PaymentAddNewCardController extends EditBaseController<PaymentAddNewCardController.Args> implements SettingsAdapter.TextChangeListener, View.OnClickListener {
  public static class Args {
    private final PaymentFormController parentController;
    private final TdApi.PaymentsProviderStripe paymentsProvider;

    public Args (PaymentFormController parentController, TdApi.PaymentsProviderStripe paymentsProvider) {
      this.parentController = parentController;
      this.paymentsProvider = paymentsProvider;
    }
  }

  private PaymentFormController parentController;
  private TdApi.PaymentsProviderStripe paymentsProvider;
  private SettingsAdapter adapter;

  @Override
  public void setArguments (Args args) {
    super.setArguments(args);
    this.parentController = args.parentController;
    this.paymentsProvider = args.paymentsProvider;
  }

  public PaymentAddNewCardController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public int getId () {
    return R.id.controller_paymentFormNewMethod;
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.PaymentFormNewMethodCard);
  }

  @Override
  protected int getRecyclerBackgroundColorId () {
    return R.id.theme_color_background;
  }

  @Override
  protected void onCreateView (Context context, FrameLayoutFix contentView, RecyclerView recyclerView) {
    setDoneIcon(R.drawable.baseline_check_24);
    setInstantDoneVisible(false);

    adapter = new SettingsAdapter(this) {
      @Override
      protected void modifyEditText (ListItem item, ViewGroup parent, MaterialEditTextGroup editText) {
        parent.setBackgroundColor(Theme.fillingColor());
        Views.setSingleLine(editText.getEditText(), true);

        switch (item.getId()) {
          case R.id.btn_inputCardExpireDate:
            editText.setMaxLength(5);
            editText.getEditText().setFilters(new InputFilter.LengthFilter[] { new InputFilter.LengthFilter(5) });
            editText.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              editText.getEditText().setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE);
            }
            break;
          case R.id.btn_inputCardNumber:
            editText.setMaxLength(16); // 14 - 16
            editText.getEditText().setFilters(new InputFilter.LengthFilter[] { new InputFilter.LengthFilter(16) });
            editText.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              editText.getEditText().setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_NUMBER);
            }
            break;
          case R.id.btn_inputCardCVV:
            editText.setMaxLength(4);
            editText.getEditText().setFilters(new InputFilter.LengthFilter[] { new InputFilter.LengthFilter(4) });
            editText.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              editText.getEditText().setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE);
            }
            break;
          default:
            editText.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            break;
        }
      }

      @Override
      protected void setValuedSetting (ListItem item, SettingView view, boolean isUpdate) {
        if (item.getId() == R.id.btn_inputCardCountry) {
          view.setData(i_cardCountryUI.isEmpty() ? Lang.getString(R.string.PaymentFormNotSet) : i_cardCountryUI);
        }
      }
    };

    adapter.setTextChangeListener(this);

    List<ListItem> items = new ArrayList<>();
    items.add(new ListItem(ListItem.TYPE_EDITTEXT_NO_PADDING, R.id.btn_inputCardNumber, 0, R.string.PaymentFormNewMethodCardNumber));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_EDITTEXT_NO_PADDING, R.id.btn_inputCardExpireDate, 0, R.string.PaymentFormNewMethodExpireDate));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));

    if (paymentsProvider.needCardholderName) {
      items.add(new ListItem(ListItem.TYPE_EDITTEXT_NO_PADDING, R.id.btn_inputCardHolder, 0, R.string.PaymentFormNewMethodCardHolder));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }

    items.add(new ListItem(ListItem.TYPE_EDITTEXT_NO_PADDING, R.id.btn_inputCardCVV, 0, R.string.PaymentFormNewMethodCVV));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

    if (paymentsProvider.needCountry || paymentsProvider.needPostalCode) {
      items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.PaymentFormNewMethodBillingSection));
      items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
      if (paymentsProvider.needCountry) {
        items.add(new ListItem(ListItem.TYPE_VALUED_SETTING, R.id.btn_inputCardCountry, 0, R.string.PaymentFormNewMethodBillingCountry));
        items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
      }
      if (paymentsProvider.needPostalCode) {
        items.add(new ListItem(ListItem.TYPE_EDITTEXT_NO_PADDING, R.id.btn_inputCardPostCode, 0, R.string.PaymentFormNewMethodBillingPostcode));
      }
      items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    }

    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_inputCardSaveInfo, 0, R.string.PaymentFormNewMethodSave));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.PaymentFormNewMethodSaveInfo));

    adapter.setItems(items, false);
    recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    recyclerView.setAdapter(adapter);
    checkDoneButton();
  }

  @Override
  public void onClick (View v) {
    if (v.getId() == R.id.btn_inputCardCountry) {
      SelectCountryController c = new SelectCountryController(context, tdlib);
      c.setArguments(new SelectCountryController.Args((country) -> {
        i_cardCountry = country.countryCode;
        i_cardCountryUI = country.countryName;
        adapter.updateValuedSettingById(R.id.btn_inputCardCountry);
        checkDoneButton();
      }));
      navigateTo(c);
    } else if (v.getId() == R.id.btn_inputCardSaveInfo) {

    }
  }

  // fields
  private String i_cardNumber = "";
  private String i_cardExpireDate = "";
  private String i_cardCvv = "";
  private String i_cardHolder = "";
  private String i_cardCountry = "";
  private String i_cardPostcode = "";

  private String i_cardCountryUI = "";
  //

  @Override
  public void onTextChanged (int id, ListItem item, MaterialEditTextGroup v, String text) {
    if (id == R.id.btn_inputCardExpireDate) {
      Log.d("[onTextChanged] %s", text);

      if (text.length() == 1 && text.charAt(0) != '/') {
        int textAsInt = Integer.parseInt(text);
        if (textAsInt != 0 && textAsInt != 1) {
          v.getEditText().setText("0" + text + "/");
          v.getEditText().setSelection(3);
        }
      } else if (text.length() == 2 && i_cardExpireDate.length() < text.length()) {
        v.getEditText().setText(text + "/");
        v.getEditText().setSelection(3);
      } else if (text.length() == 2 && i_cardExpireDate.length() > text.length()) {
        v.getEditText().setText(String.valueOf(text.charAt(0)));
        v.getEditText().setSelection(1);
      }

      // verify
      if (v.getText().length() == 5) {
        String[] dateParts = v.getText().toString().split("/");
        int month = Integer.parseInt(dateParts[0]);
        int year = Integer.parseInt(dateParts[1]);
        boolean valid = month > 0 && month < 13 && !DateUtils.hasMonthPassed(year, month) && !DateUtils.hasYearPassed(year);
        v.setInErrorState(!valid);
      }

      i_cardExpireDate = text;
      checkDoneButton();
      return;
    }

    switch (id) {
      case R.id.btn_inputCardNumber:
        i_cardNumber = text;
        break;
      case R.id.btn_inputCardCVV:
        i_cardCvv = text;
        break;
      case R.id.btn_inputCardHolder:
        i_cardHolder = text;
        break;
      case R.id.btn_inputCardPostCode:
        i_cardPostcode = text;
        break;
    }

    checkDoneButton();
  }

  @Override
  protected boolean onDoneClick () {
    setDoneInProgress(true);

    try {
      tokenizeCard();
    } catch (Exception e) {
      Log.e(e);
      showAlert(new AlertDialog.Builder(context).setTitle(R.string.Error).setMessage(e.getMessage()));
      setDoneInProgress(false);
    }

    return true;
  }

  private void checkDoneButton () {
    boolean isValid = i_cardNumber.length() >= 14 && i_cardCvv.length() >= 3 && i_cardExpireDate.length() == 5;
    if (paymentsProvider.needCardholderName && i_cardHolder.isEmpty()) isValid = false;
    if (paymentsProvider.needCountry && i_cardCountry.isEmpty()) isValid = false;
    if (paymentsProvider.needPostalCode && i_cardPostcode.isEmpty()) isValid = false;
    setDoneVisible(isValid);
  }

  private void vibrateError (View target) {
    UI.hapticVibrate(target, true);
  }

  private void tokenizeCard () throws AuthenticationException {
    String[] expDate = i_cardExpireDate.split("/");

    Card stripeCard = new Card.Builder(i_cardNumber, Integer.valueOf(expDate[0]), Integer.valueOf(expDate[1]), i_cardCvv)
            .name(paymentsProvider.needCardholderName ? i_cardHolder : null)
            .addressCountry(paymentsProvider.needCountry ? i_cardCountry : null)
            .addressZip(paymentsProvider.needPostalCode ? i_cardPostcode : null)
            .build();

    if (!stripeCard.validateNumber()) {
      adapter.indexOfView()
      return;
    }

    if (!stripeCard.validateExpiryDate()) {

      return;
    }

    if (!stripeCard.validateCVC()) {

      return;
    }

    // validate

    new Stripe(paymentsProvider.publishableKey).createToken(stripeCard, new TokenCallback() {
      @Override
      public void onSuccess (Token token) {

      }

      @Override
      public void onError (Exception error) {

      }
    });
  }
}