package metatype.deepstate.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.Test;

import metatype.deepstate.example.Toaster.ToasterDial;

public class ToasterTest {
  private final Toaster toaster = new Toaster(Duration.ofSeconds(30));

  @Test
  public void testInitialToasting() {
    assertThat(toaster.isToasting()).isFalse();
  }

  @Test
  public void testInitialDialSetting() {
    toaster.getToasterSetting((setting) -> {
      assertThat(setting).isEqualTo(ToasterDial.FIVE);
    }); 
  }

  @Test
  public void testSetDial() {
    toaster.changeToasterSetting(ToasterDial.THREE);
    toaster.getToasterSetting((setting) -> {
      assertThat(setting).isEqualTo(ToasterDial.THREE);
    }); 
  }
  
  @Test
  public void testMakeToast() throws InterruptedException {
    toaster.changeToasterSetting(ToasterDial.ONE);
    toaster.makeToast();
    
    Thread.sleep(1000);
    assertThat(toaster.isToasting()).isFalse();
  }

  @Test
  public void testCancelToasting() throws InterruptedException {
    toaster.changeToasterSetting(ToasterDial.ELEVEN);
    toaster.makeToast();
    assertThat(toaster.isToasting()).isTrue();

    toaster.pressCancel();
    assertThat(toaster.isToasting()).isFalse();
  }

  @Test
  public void testChangeDialWhileToasting() throws InterruptedException {
    toaster.changeToasterSetting(ToasterDial.ELEVEN);
    toaster.makeToast();
    toaster.changeToasterSetting(ToasterDial.ONE);

    Thread.sleep(1000);
    assertThat(toaster.isToasting()).isFalse();
  }

  @Test
  public void testBreakToaster() throws InterruptedException {
    toaster.changeToasterSetting(ToasterDial.ONE);
    toaster.jamLever();
    toaster.makeToast();
    
    Thread.sleep(1000);
    assertThat(toaster.isToasting()).isTrue();
    
    Thread.sleep(31000);
    assertThat(toaster.isBurning()).isTrue();
  }

  @Test
  public void testBurnToast() throws InterruptedException {
    toaster.changeToasterSetting(ToasterDial.ELEVEN);
    toaster.makeToast();
    
    Thread.sleep(31000);
    assertThat(toaster.isBurning()).isTrue();
  }
}
