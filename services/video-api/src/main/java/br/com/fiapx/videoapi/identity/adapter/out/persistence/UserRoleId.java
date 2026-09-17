package br.com.fiapx.videoapi.identity.adapter.out.persistence;

import java.io.Serializable;
import java.util.UUID;

record UserRoleId(UUID userId, String role) implements Serializable {
}
