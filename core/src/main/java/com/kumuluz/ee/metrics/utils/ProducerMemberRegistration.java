package com.kumuluz.ee.metrics.utils;

import org.eclipse.microprofile.metrics.Metadata;

import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.Bean;

/**
 * Information needed for producer member metric registration.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 */
public class ProducerMemberRegistration {

    private Bean bean;
    private AnnotatedMember member;
    private Metadata metadata;

    public ProducerMemberRegistration(Bean bean, AnnotatedMember member, Metadata metadata) {
        this.bean = bean;
        this.member = member;
        this.metadata = metadata;
    }

    public Bean getBean() {
        return bean;
    }

    public AnnotatedMember getMember() {
        return member;
    }

    public Metadata getMetadata() {
        return metadata;
    }
}
