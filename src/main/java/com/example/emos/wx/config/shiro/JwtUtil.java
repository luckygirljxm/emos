package com.example.emos.wx.config.shiro;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.emos.wx.exception.EmosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
@Slf4j
@Component
public class JwtUtil {

    //密钥
    @Value("${emos.jwt.secret}")
    public String secret;

    @Value("${emos.jwt.expire}")
    private int expire;

    /**
     * 获得token token = userId + 过期时间 + jwt密钥
     *
     * @param userId
     * @return
     */
    public String createToken(int userId) {
        //获得时间
        Date date = DateUtil.offset(new Date(), DateField.DAY_OF_YEAR, expire).toJdkDate();
        //创建加密算法对象  secret配置类中设置的密钥   因为我们没有设置登录密码
        Algorithm algorithm = Algorithm.HMAC256(secret);
        JWTCreator.Builder builder = JWT.create();
        String token = builder.withClaim("userId", userId)
                .withExpiresAt(date).sign(algorithm);

        return token;
    }

    /**
     * 通过token来得到userId
     *
     * @param token 登录凭证
     * @return
     */
    //为什么需要这个方法
    public int getUserId(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("userId").asInt();

        } catch (Exception e) {
            throw new EmosException("令牌无效");
        }

    }


    /**
     * 验证token的有效性
     * @param token
     */
    public void verifierToken(String token){
        Algorithm algorithm = Algorithm.HMAC256(secret);
        JWTVerifier verifier = JWT.require(algorithm).build();
        //没有retrun 因为如果异常会直接抛出错误信息
        verifier.verify(token);
    }
}
