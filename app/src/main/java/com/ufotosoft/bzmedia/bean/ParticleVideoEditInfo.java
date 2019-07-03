package com.ufotosoft.bzmedia.bean;

/**
 * Created by zhandalin on 2018-04-18 17:42.
 * 说明:
 */
public class ParticleVideoEditInfo extends VideoEditInfo {
    private ParticleBean particleBean;
    private String particleConfig;

    public String getParticleConfig() {
        return particleConfig;
    }

    public void setParticleConfig(String particleConfig) {
        this.particleConfig = particleConfig;
    }

    public ParticleBean getParticleBean() {
        return particleBean;
    }

    public void setParticleBean(ParticleBean particleBean) {
        this.particleBean = particleBean;
    }
}
