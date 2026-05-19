package br.edu.chat.core;

import br.edu.chat.model.User;
import br.edu.chat.protocol.ServerResponse;
import br.edu.chat.repository.BlockRepository;
import br.edu.chat.repository.UserRepository;

public class BlockService {

    private final BlockRepository blockRepo;
    private final UserRepository userRepo;

    public BlockService(BlockRepository blockRepo, UserRepository userRepo) {
        this.blockRepo = blockRepo;
        this.userRepo = userRepo;
    }

    public String block(int blockerId, String targetLogin) {
        User target = userRepo.findByLogin(targetLogin);
        if (target == null) {
            return ServerResponse.error("Usuario '" + targetLogin + "' nao encontrado.");
        }
        if (target.getId() == blockerId) {
            return ServerResponse.error("Voce nao pode bloquear a si mesmo.");
        }

        boolean ok = blockRepo.blockUser(blockerId, target.getId());
        if (!ok) {
            return ServerResponse.error("Nao foi possivel bloquear o usuario (ja bloqueado?).");
        }

        return ServerResponse.ok("Usuario '" + targetLogin + "' bloqueado com sucesso.");
    }

    public String unblock(int blockerId, String targetLogin) {
        User target = userRepo.findByLogin(targetLogin);
        if (target == null) {
            return ServerResponse.error("Usuario '" + targetLogin + "' nao encontrado.");
        }

        boolean ok = blockRepo.unblockUser(blockerId, target.getId());
        if (!ok) {
            return ServerResponse.error("Nao foi possivel desbloquear o usuario (nao estava bloqueado?).");
        }

        return ServerResponse.ok("Usuario '" + targetLogin + "' desbloqueado com sucesso.");
    }

    public boolean isBlockedBetween(int userId1, int userId2) {
        return blockRepo.isBlockedBetween(userId1, userId2);
    }
}
