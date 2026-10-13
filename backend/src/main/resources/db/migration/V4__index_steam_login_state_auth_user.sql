create index if not exists steam_login_states_auth_user_id_idx
  on private.steam_login_states (auth_user_id);
