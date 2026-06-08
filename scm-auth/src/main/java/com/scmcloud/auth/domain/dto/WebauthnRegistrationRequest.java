package com.scmcloud.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * WebAuthn 娉ㄥ唽璇锋眰DTO
 * <p>
 * 鐢ㄤ簬娉ㄥ唽鏂扮殑WebAuthn鍑瘉
 * 鍙傝€僃IDO2瑙勮寖鍜孏oogle Passkey瀹炵幇
 *
 * @author system
 * @since 2025-11-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebauthnRegistrationRequest {
    @NotBlank(message = "鍑瘉 ID涓嶈兘涓虹┖")
    @Size(min = 16, max = 1024, message = "鍑瘉ID闀垮害蹇呴』锟?-1024涔嬮棿")
    private String credentialId;

    @NotBlank(message = "clientDataJSON 涓嶈兘涓虹┖")
    private String clientDataJSON;

    @NotBlank(message = "attestationObject 涓嶈兘涓虹┖")
    private String attestationObject;

    @NotBlank(message = "璁惧 ID涓嶈兘涓虹┖")
    private String deviceId;

    @Size(max = 2048, message = "鍏挜闀垮害涓嶈兘瓒呰繃2048")
    private String publicKeyPem;

    @NotBlank(message = "绛惧悕绠楁硶涓嶈兘涓虹┖")
    @Pattern(regexp = "^(ES256|ES384|ES512|RS256|RS384|RS512|PS256|PS384|PS512|EdDSA)$",
             message = "涓嶆敮鎸佺殑绛惧悕绠楁硶")
    private String algorithm;

    @Size(max = 100, message = "璁惧鍚嶇О闀垮害涓嶈兘瓒呰繃100")
    private String deviceName;

    private UUID aaguid;

    @Size(max = 100, message = "浼犺緭鏂瑰紡闀垮害涓嶈兘瓒呰繃100")
    private String transports;

    @Pattern(regexp = "^(platform|cross-platform)$", message = "璁よ瘉鍣ㄧ被鍨嬩笉姝ｇ‘")
    private String authenticatorAttachment;

    @Pattern(regexp = "^(required|preferred|discouraged)$", message = "鐢ㄦ埛楠岃瘉鏂规硶涓嶆纭?)
    private String userVerification;

    private Boolean backupState;

    private Boolean backupEligible;
}