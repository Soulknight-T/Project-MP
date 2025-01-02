package edu.illinois.cs.cs124.ay2022.mp.models;

/*
 * Class allowing us to return both a result and an exception from a method that might throw.
 * The code here should look familiar, except for the type parameter T.
 * But we'll cover that in a later lesson.
 *
 * The reason that we are using this class is somewhat complicated, and related to the internals
 * of how Android performs networking requests.
 * Feel free to skip this explanation, or don't worry if you don't understand it completely.
 * We'll talk a bit about this when we discuss networking in Android during MP2.
 *
 * Until now our model of computation has been that there is a single stream of tasks that the
 * computer is performing.
 * However, this model is inaccurate.
 * In reality, your computer is usually doing multiple things at once, and in many applications
 * there can be multiple parts of the code that are being executed simultaneously.
 * This can be extremely important for performance, and is the only way to take advantage of modern
 * computer processors, which themselves can be executing multiple tasks simultaneously.
 *
 * Being able to do two things at the same time is also extremely important for interactive
 * user-facing applications, as a way to hide slow tasks from the user.
 * Imagine you go to a restaurant, but there is only one person working there, who has to take
 * orders, cook, wait tables, wash dishes, and so on.
 * You may need to wait a while before you can place your order, because they are busy doing so
 * many other things.
 *
 * Certain operations in Android can take a long time.
 * Making a network request is one of them.
 * As a result, Android requires that apps make network requests in a way that allows the UI to
 * remain responsive.
 * If it did not, a slow API call would cause the entire app to freeze until it completed.
 * Users find this frustrating.
 *
 * Using our analogy above, instead of having one person trying to do everything, Android requires
 * that when a network request is made, the person who is taking orders hand that task off to a
 * helper, so that they can remain at the counter greeting guests and taking orders.
 * (This is how most restaurants actually work.)
 * The helper performs the slow network request, and then provides a notification when it completes.
 * This is why we provide a callback method to our API Client in our MainActivity.
 *
 * The only problem with this model is that, because the help is performing the network request,
 * the exception will be thrown up the helper's stack, and not in the context of the UI thread.
 * Sometimes this is fine, but use of the class below allows the helper to pass back a result that
 * includes either the result of performing the network request (if it succeeded), or an exception
 * (if it failed).
 * Returning to our restaurant analogy, this is similar to having the helper return to the counter
 * if something goes wrong—for example, they can't complete the order because they are out of an
 * ingredient, or whatever.
 *
 * Concurrent programming is a fascinating topic and something that you'll have a chance to tangle
 * with more in later courses and in real-world programming tasks.
 * We are not trying to teach these concepts, but building Android apps does inevitably bring us
 * into contact with a few of these ideas.
 */
public class ResultMightThrow<T> {
  private final T result;
  private final Exception exception;

  // Constructor used on success
  public ResultMightThrow(final T setResult) {
    result = setResult;
    exception = null;
  }

  // Constructor used on failure
  public ResultMightThrow(final Exception setException) {
    result = null;
    exception = setException;
  }

  // Retrieving the result will throw if the operation failed
  public T getResult() {
    if (exception != null) {
      throw new RuntimeException(exception);
    } else {
      return result;
    }
  }

  // Allow retrieving the exception
  public Exception getException() {
    return exception;
  }
}
