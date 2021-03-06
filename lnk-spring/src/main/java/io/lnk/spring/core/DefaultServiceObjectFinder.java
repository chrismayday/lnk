package io.lnk.spring.core;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.ConcurrentReferenceHashMap;

import io.lnk.api.InvokerCommand;
import io.lnk.api.annotation.LnkServiceVersion;
import io.lnk.api.exception.NotFoundServiceException;
import io.lnk.core.ServiceObjectFinder;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月23日 下午4:44:10
 */
public class DefaultServiceObjectFinder implements ServiceObjectFinder, BeanFactoryAware, BeanClassLoaderAware {
    private static final Logger log = LoggerFactory.getLogger(DefaultServiceObjectFinder.class.getSimpleName());
    private final ConcurrentReferenceHashMap<String, Object> serviceObjects = new ConcurrentReferenceHashMap<String, Object>(256);
    private ListableBeanFactory beanFactory;
    private ClassLoader classLoader;

    @Override
    public void registry(String serviceGroup, String serviceId, String version, int protocol, Object bean) {
        this.serviceObjects.put(this.buildServiceObjectKey(serviceGroup, serviceId, version, protocol), bean);
    }

    @Override
    public Object getServiceObject(InvokerCommand command) throws NotFoundServiceException {
        String serviceGroup = command.getServiceGroup();
        String serviceId = command.getServiceId();
        String version = command.getVersion();
        int protocol = command.getProtocol();
        Object serviceBean = this.serviceObjects.get(this.buildServiceObjectKey(serviceGroup, serviceId, version, protocol));
        if (serviceBean != null) {
            return serviceBean;
        }
        Class<?> serviceInterface = null;
        try {
            serviceInterface = this.classLoader.loadClass(serviceId);
        } catch (ClassNotFoundException e) {
            log.error("load class " + serviceId + " Error.", e);
            throw new NotFoundServiceException(serviceId, e);
        }
        try {
            Map<String, ?> beans = beanFactory.getBeansOfType(serviceInterface);
            for (Map.Entry<String, ?> e : beans.entrySet()) {
                Object bean = e.getValue();
                Class<?> beanType = bean.getClass();
                if (beanType.isAnnotationPresent(LnkServiceVersion.class)) {
                    LnkServiceVersion lnkServiceVersion = beanType.getAnnotation(LnkServiceVersion.class);
                    if (StringUtils.equals(lnkServiceVersion.version(), version)) {
                        serviceBean = bean;
                        break;
                    }
                }
            }
            if (serviceBean == null) {
                throw new NotFoundServiceException(serviceId);
            }
            this.registry(serviceGroup, serviceId, version, protocol, serviceBean);
            return serviceBean;
        } catch (NotFoundServiceException e) {
            log.error("bean class " + serviceId + " is not exists.", e);
            throw e;
        } catch (Throwable e) {
            log.error("find bean class " + serviceId + " Error.", e);
            throw new NotFoundServiceException(serviceId, e);
        }
    }

    private String buildServiceObjectKey(String serviceGroup, String serviceId, String version, int protocol) {
        return new StringBuffer(serviceGroup).append(".").append(serviceId).append(".").append(version).append(".").append(protocol).toString();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ListableBeanFactory) beanFactory;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}
