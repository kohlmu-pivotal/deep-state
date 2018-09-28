package metatype.deepstate.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.Test;

import metatype.deepstate.example.Toaster.Dial;

public abstract class ToasterTest {
  private Toaster toaster;

  public ToasterTest() {
    toaster = create(Duration.ofSeconds(30));
  }
  
  public abstract Toaster create(Duration autoIgnite);

  @Test
  public void testInitialToasting() {
    assertThat(toaster.isToasting()).isFalse();
  }

  @Test
  public void testInitialDialSetting() {
    assertThat(toaster.getToasterSetting()).isEqualTo(Dial.FIVE);
  }

  @Test
  public void testSetDial() {
    toaster.changeToasterSetting(Dial.THREE);
    assertThat(toaster.getToasterSetting()).isEqualTo(Dial.THREE);
  }
  
  @Test
  public void testMakeToast() throws InterruptedException {
    toaster.changeToasterSetting(Dial.ONE);
    toaster.depressLever();
    
    Thread.sleep(1000);
    assertThat(toaster.isToasting()).isFalse();
  }

  @Test
  public void testCancelToasting() throws InterruptedException {
    toaster.changeToasterSetting(Dial.ELEVEN);
    toaster.depressLever();
    assertThat(toaster.isToasting()).isTrue();

    toaster.pressCancel();
    assertThat(toaster.isToasting()).isFalse();
  }

  @Test
  public void testChangeDialWhileToasting() throws InterruptedException {
    toaster.changeToasterSetting(Dial.ELEVEN);
    toaster.depressLever();
    toaster.changeToasterSetting(Dial.ONE);

    Thread.sleep(1000);
    assertThat(toaster.isToasting()).isFalse();
  }

  @Test
  public void testBreakToaster() throws InterruptedException {
    toaster.changeToasterSetting(Dial.ONE);
    toaster.jamLever();
    toaster.depressLever();
    
    Thread.sleep(1000);
    assertThat(toaster.isToasting()).isTrue();
    
    Thread.sleep(31000);
    assertThat(toaster.isBurning()).isTrue();
  }

  @Test
  public void testBurnToast() throws InterruptedException {
    toaster.changeToasterSetting(Dial.ELEVEN);
    toaster.depressLever();
    
    Thread.sleep(31000);
    assertThat(toaster.isBurning()).isTrue();
  }
}
