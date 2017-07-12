package easyrmi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for a remote reflective {@link InvocationHandler invocation handler}. This class will handle invocation of
 * methods not in the declared remote interface (such as methods inherited from {@link Object}) and forward the remote method
 * invocations to the abstract {@code invokeRemote(Method, Object[])} method which descendants must implement.
 * @author ReneAndersen
 */
abstract class RemoteInvocationHandler implements InvocationHandler {
  private static final Logger logger = LoggerFactory.getLogger(RemoteInvocationHandler.class);

  private final Class<?> classType;
  private final List<Method> apiMethods;

  /**
   * Create a proxy invocation handler for a remote interface.
   * @param classType the class-type for the remote interface
   */
  protected RemoteInvocationHandler(final Class<?> classType) {
    this.classType = classType;
    this.apiMethods = Arrays.asList(classType.getMethods());
  }

  /**
   * @return the class type of the remote interface.
   */
  protected Class<?> getClassType() {
    return classType;
  }

  /** {@inheritDoc} */
  @Override
  public Object invoke(final Object proxy,
                       final Method method,
                       final Object[] args) throws Throwable {
    try {
      if (apiMethods.contains(method)) {
        return invokeRemote(method, args);
      }
      if (method.getDeclaringClass().isAssignableFrom(getClass())) {
        return method.invoke(this, args);
      }
      throw new UnsupportedOperationException("Unsupported operation on remote class of type '" + classType + ": " + method); //$NON-NLS-1$ //$NON-NLS-2$
    } catch (final InvocationTargetException e) {
      throw e.getTargetException();
    } catch (final Exception e) {
      if (logger.isDebugEnabled()) logger.debug("Error in remote method invocation: " + toString(method, args), e); //$NON-NLS-1$
      throw getMethodCompatibleException(method, e);
    }
  }

  @Override
  public String toString() {
    return "RemoteInvocationHandler [classType=" + classType.getName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * Called if the method being invoked is in the remote interface.
   * @param method the method being invoked.
   * @param args the arguments to the method.
   * @return the return value from the remote method invocation.
   * @throws InvocationTargetException if the method throws an exception.
   * @throws Exception if any other exception occurs.
   */
  protected abstract Object invokeRemote(final Method method,
                                         final Object[] args) throws InvocationTargetException, Exception;

  private Exception getMethodCompatibleException(final Method method, final Exception e) {
    Exception res = null;
    for (final Class<?> exceptionType : method.getExceptionTypes()) {
      if (exceptionType.isAssignableFrom(e.getClass())) {
        res = e;
        break;
      }
      if (res == null && exceptionType.isAssignableFrom(RemoteException.class)) {
        res = new RemoteException(e.getMessage(), e);
      }
    }
    if (res == null) {
      res = new RuntimeException(e.getMessage(), e);
    }
    return res;
  }

  private String toString(final Method method, final Object[] args) {
    return classType.getName() + "." + method.getName() + Arrays.toString(args); //$NON-NLS-1$
  }
}