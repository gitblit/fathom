## About

**Fathom-Eventbus** provides an injectable [Guava Eventbus](https://code.google.com/p/guava-libraries/wiki/EventBusExplained) singleton instance for decoupled event passing.

## Installation

Add the **Fathom-Eventbus** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-eventbus</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

## Configuration

None.

## Usage

Bind your publisher & subscriber objects.

```java
public class Components extends ComponentsModule {

  @Override
  protected void init() {
    bind(MyDao.class);
    bind(MyEventsSubscriber.class);
  }

}
```

In this example we inject the Eventbus into our *subscriber* object so that it can register itself.

```java
@Singleton
public class MyEventsSubscriber {

    private final static Logger log = LoggerFactory.getLogger(MyEventsSubscriber.class);

    @Inject
    public MyEventsSubscriber(EventBus eventBus) {
        eventBus.register(this);
    }

    // the @Subscribe annotation directs the eventbus
    // to register this instance as a listener of ChangeEvent
    @Subscribe
    public void handleCustomerChange(ChangeEvent changeEvent) {
        log.info("A customer was changed");
    }
}
```

Your *publisher* object requires the Eventbus instance so that it can publish messages.

```java
@Singleton
public class MyDao {

  EventBus eventBus;

  @Inject
  public MyDao(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  public void changeCustomer() {
    // post a message of type ChangeEvent
    ChangeEvent event = getChangeEvent();
    eventBus.post(event);
  }
}
```
