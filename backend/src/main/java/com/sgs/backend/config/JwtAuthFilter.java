package com.sgs.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre JWT — intercepte CHAQUE requête HTTP entrante.
 *
 * Flow :
 *   1. Lit le header "Authorization: Bearer <token>"
 *   2. Extrait l'email du token
 *   3. Charge l'utilisateur depuis la BDD via UserDetailsService
 *   4. Valide le token (signature + date d'expiration)
 *   5. Vérifie que le compte est toujours actif (isEnabled)
 *   6. Si tout est OK → met l'utilisateur dans le SecurityContext, avec les
 *      rôles relus en base (pas ceux du token — voir étape 8)
 *      → les controllers peuvent ensuite utiliser @AuthenticationPrincipal
 *        et @PreAuthorize
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Lire le header Authorization
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Pas de token → on laisse passer la requête
            // (Spring Security décidera si elle est autorisée ou non)
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraire le token (enlever "Bearer " au début)
        final String jwt = authHeader.substring(7);

        try {
            // 3. Extraire l'email du token
            final String email = jwtUtil.extractEmail(jwt);

            // 4. Si on a un email et que le SecurityContext est vide
            //    (l'utilisateur n'est pas encore authentifié dans cette requête)
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 5. Charger l'utilisateur depuis la BDD
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 6. Valider le token (signature correcte + non expiré)
                if (jwtUtil.validateToken(jwt, userDetails)) {

                    // 7. Compte toujours actif ? Le token ne porte pas l'état du
                    //    compte (24h de validité, pas de révocation) : sans ce
                    //    contrôle, un utilisateur désactivé garderait l'accès API
                    //    jusqu'à expiration de son token. isEnabled() reflète
                    //    utilisateur.actif relu en base à cette requête (voir
                    //    UtilisateurService.loadUserByUsername).
                    //
                    //    On ne lève PAS d'exception ici : ce filtre court AVANT
                    //    ExceptionTranslationFilter, une DisabledException ne
                    //    serait ni traduite ni attrapée proprement (elle tomberait
                    //    dans le catch générique plus bas ou finirait en 500).
                    //    On s'abstient simplement d'authentifier — la requête est
                    //    alors traitée comme non connectée (401/403 propre),
                    //    exactement comme un token invalide.
                    if (userDetails.isEnabled()) {

                        // 8. Créer l'objet d'authentification Spring Security.
                        //
                        //    Les autorités viennent de userDetails (donc de la BDD,
                        //    rechargée à l'étape 5) et NON du claim "role" du token.
                        //    La distinction est importante depuis que @PreAuthorize
                        //    garde chaque endpoint : le token est signé pour 24h et
                        //    il n'existe pas encore de mécanisme de révocation, donc
                        //    se fier à son claim laisserait un utilisateur rétrogradé
                        //    (ADMIN -> VENDEUR) conserver ses droits jusqu'à
                        //    l'expiration. En relisant le rôle en base, un changement
                        //    prend effet dès la requête suivante.
                        //
                        //    Le claim "role" reste dans le token : le frontend s'en
                        //    sert pour afficher/masquer l'UI. Il est commode côté
                        //    client, il n'est simplement pas la source d'autorité
                        //    côté serveur.
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        // 9. Poser l'authentification dans le SecurityContext
                        //    → tous les controllers/filters suivants peuvent y accéder
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                    } else {
                        // Traçabilité : un token valide utilisé par un compte
                        // désactivé est un signal digne d'intérêt dans les logs.
                        logger.warn("Compte désactivé, token refusé : " + email);
                    }
                }
            }
        } catch (Exception e) {
            // Token invalide ou erreur d'extraction → on ne met rien dans le SecurityContext
            // La requête continuera sans authentification
            logger.warn("JWT invalide : " + e.getMessage());
        }

        // 10. Passer au filtre suivant dans la chaîne
        filterChain.doFilter(request, response);
    }
}
